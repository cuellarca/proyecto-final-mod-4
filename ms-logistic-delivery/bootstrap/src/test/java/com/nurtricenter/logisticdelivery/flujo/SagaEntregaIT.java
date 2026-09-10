package com.nurtricenter.logisticdelivery.flujo;

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
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * El Outbox se drena a
 * RabbitMQ y los eventos llegan a los consumidores stub. Demuestra la <b>saga de fallo</b>:
 * EntregaFallida -> ms-notificaciones; y agotados los reintentos, EntregaNoConcretada -> ms-catering.
 * Levanta PostGIS y RabbitMQ con Testcontainers.
 */
@PruebaDeIntegracion
class SagaEntregaIT {

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
    @DisplayName("HU-4 · flujo correcto: EntregaConfirmada llega a ms-notificaciones por RabbitMQ")
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
    @DisplayName("HU-5 · flujo incorrecto: agotados los reintentos la saga termina en ms-catering como no concretada")
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
