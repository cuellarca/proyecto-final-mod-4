package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntregaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallida;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallidaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntregaCommand;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.CateringStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.NotificacionesStubListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Fase 4 (verificacion de integracion contra PostGIS + RabbitMQ externos): el Outbox se drena a
 * RabbitMQ y los eventos llegan a los consumidores stub. Demuestra la <b>saga de fallo</b>:
 * EntregaFallida -> ms-notificaciones; y agotados los reintentos, EntregaNoConcretada -> ms-catering.
 * Requiere Postgres en 5435 y RabbitMQ en 5672.
 */
@EnabledIfSystemProperty(named = "msld.external.db", matches = "true")
@SpringBootTest(
        classes = LogisticDeliveryApplication.class,
        properties = {
                "logistic.reintentos.maximo=1",
                "logistic.outbox.poll-delay-ms=500"
        })
class Fase4SagaExternaManualIT {

    @Autowired
    PlanificarRutaDelDia planificarRutaDelDia;
    @Autowired
    ConfirmarEntrega confirmarEntrega;
    @Autowired
    RegistrarEntregaFallida registrarEntregaFallida;
    @Autowired
    ReintentarEntrega reintentarEntrega;
    @Autowired
    NotificacionesStubListener notificaciones;
    @Autowired
    CateringStubListener catering;

    private String programarEntrega(String repartidor, String paquete, String paciente, LocalDate fecha) {
        PlanificarRutaResultado plan = planificarRutaDelDia.ejecutar(new PlanificarRutaDelDiaCommand(
                repartidor, fecha, -17.78, -63.18,
                List.of(new PlanificarRutaDelDiaCommand.Paquete(paquete, paciente, -17.79, -63.19))));
        return plan.entregas().get(0).entregaId();
    }

    @Test
    void entregaConfirmadaLlegaAMsNotificaciones() {
        String paciente = "pac-notif-1";
        String entregaId = programarEntrega("rep-n1", "pkg-n1", paciente, LocalDate.of(2026, 7, 14));

        confirmarEntrega.ejecutar(new ConfirmarEntregaCommand(
                entregaId, -17.79, -63.19, "https://s/e.jpg", "Receptor"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificaciones.recibidos())
                        .anySatisfy(e -> {
                            assertThat(e.type()).isEqualTo("EntregaConfirmada");
                            assertThat(e.payload().path("pacienteId").asText()).isEqualTo(paciente);
                        }));
    }

    @Test
    void sagaDeFalloTerminaEnMsCateringComoNoConcretada() {
        String paciente = "pac-catering-1";
        String entregaId = programarEntrega("rep-c1", "pkg-c1", paciente, LocalDate.of(2026, 7, 15));

        // maximo=1: fallar -> reintentar (reprograma) -> fallar -> reintentar (no queda saldo -> no concretada)
        registrarEntregaFallida.ejecutar(new RegistrarEntregaFallidaCommand(entregaId, MotivoFallo.AUSENTE));
        reintentarEntrega.ejecutar(new ReintentarEntregaCommand(entregaId));
        registrarEntregaFallida.ejecutar(new RegistrarEntregaFallidaCommand(entregaId, MotivoFallo.AUSENTE));
        reintentarEntrega.ejecutar(new ReintentarEntregaCommand(entregaId));

        // ms-notificaciones recibio los fallos
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificaciones.recibidos())
                        .anySatisfy(e -> {
                            assertThat(e.type()).isEqualTo("EntregaFallida");
                            assertThat(e.payload().path("pacienteId").asText()).isEqualTo(paciente);
                        }));

        // ms-catering-suscripcion recibio la no-concretada (reprogramacion a dia futuro)
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(catering.recibidos())
                        .anySatisfy(e -> {
                            assertThat(e.type()).isEqualTo("EntregaNoConcretada");
                            assertThat(e.payload().path("pacienteId").asText()).isEqualTo(paciente);
                        }));
    }
}
