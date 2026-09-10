package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntregaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaRepository;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Planificar la ruta del dia (crea la
 * ruta y las entregas) y confirmar una entrega deja la {@code Entrega} ENTREGADA con su constancia y
 * una fila {@code EntregaConfirmada} en el Outbox, en la misma transaccion. Levanta PostGIS con Testcontainers.
 */
@PruebaDeIntegracion
class FlujoPersistenciaOutboxIT {

    @Autowired
    PlanificarRutaDelDia planificarRutaDelDia;
    @Autowired
    ConfirmarEntrega confirmarEntrega;
    @Autowired
    EntregaRepository entregaRepository;
    @Autowired
    OutboxJpaRepository outbox;

    @Test
    @DisplayName("HU-4 · flujo correcto: planificar y confirmar persiste la entrega y deja el evento en el Outbox")
    void planificarYConfirmarPersisteLaEntregaYDejaElEventoEnOutbox() {
        PlanificarRutaResultado plan = planificarRutaDelDia.ejecutar(new PlanificarRutaDelDiaCommand(
                "rep-77", LocalDate.of(2026, 7, 10), -17.78, -63.18,
                List.of(new PlanificarRutaDelDiaCommand.Paquete("pkg-77", "pac-77", -17.79, -63.19))));

        assertThat(plan.entregas()).hasSize(1);
        String entregaId = plan.entregas().get(0).entregaId();

        confirmarEntrega.ejecutar(new ConfirmarEntregaCommand(
                entregaId, -17.79, -63.19, "https://storage.example.com/pkg-77.jpg", "Juan Perez"));

        Entrega entrega = entregaRepository.buscarPorId(EntregaId.de(entregaId)).orElseThrow();
        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(entrega.constancia()).isPresent();

        List<OutboxJpaEntity> filas = outbox.findAll();
        assertThat(filas)
                .anySatisfy(f -> {
                    assertThat(f.getEventType()).isEqualTo("EntregaConfirmada");
                    assertThat(f.getAggregateId()).isEqualTo(entregaId);
                    assertThat(f.getPayload()).contains(entregaId);
                });
        assertThat(filas).anySatisfy(f -> assertThat(f.getEventType()).isEqualTo("RutaDeEntregaPlanificada"));
    }
}
