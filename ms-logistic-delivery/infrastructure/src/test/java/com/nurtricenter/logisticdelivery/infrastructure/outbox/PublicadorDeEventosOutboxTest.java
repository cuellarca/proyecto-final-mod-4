package com.nurtricenter.logisticdelivery.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nurtricenter.logisticdelivery.domain.entrega.ConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaConfirmada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaFallida;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import com.nurtricenter.logisticdelivery.infrastructure.config.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox transaccional: el adaptador escribe cada evento de dominio como una fila, dentro de la
 * misma transaccion del caso de uso. El repositorio Spring Data es el seam (testing-rules.md,
 * LIMITES); los eventos de dominio son reales y el ObjectMapper es el mismo de produccion.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorDeEventosOutboxTest {

    private static final EntregaId ENTREGA = EntregaId.nuevo();
    private static final Instant MOMENTO = Instant.parse("2026-07-10T12:00:00Z");

    @Mock
    OutboxJpaRepository outbox;
    @Captor
    ArgumentCaptor<OutboxJpaEntity> filaCaptor;

    /** El mismo ObjectMapper que arma Spring en produccion (ids serializados como texto). */
    private ObjectMapper objectMapperDeProduccion() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JacksonConfig().idsComoTextoModule());
    }

    private EntregaConfirmada entregaConfirmada() {
        return new EntregaConfirmada(ENTREGA, PacienteId.de("pac-1"), PaqueteId.de("pkg-1"),
                new ConstanciaDeEntrega(MOMENTO, new Geolocalizacion(-17.78, -63.18),
                        Url.de("https://storage/e.jpg"), "Ana Lopez"),
                MOMENTO);
    }

    @Test
    @DisplayName("La fila lleva el nombre del evento como tipo (published language)")
    void guardaElTipoDelEvento() {
        new PublicadorDeEventosOutbox(outbox, objectMapperDeProduccion())
                .publicar(List.of(entregaConfirmada()));

        verify(outbox).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getEventType()).isEqualTo("EntregaConfirmada");
    }

    @Test
    @DisplayName("La fila guarda el id del agregado que emitio el evento, para trazabilidad")
    void guardaElAggregateIdDelEvento() {
        new PublicadorDeEventosOutbox(outbox, objectMapperDeProduccion())
                .publicar(List.of(entregaConfirmada()));

        verify(outbox).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getAggregateId()).isEqualTo(ENTREGA.toString());
    }

    @Test
    @DisplayName("El payload viaja como JSON con la evidencia de la entrega")
    void serializaElEventoAJson() {
        new PublicadorDeEventosOutbox(outbox, objectMapperDeProduccion())
                .publicar(List.of(entregaConfirmada()));

        verify(outbox).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getPayload())
                .contains("\"nombreReceptor\":\"Ana Lopez\"")
                .contains("\"entregaId\":\"" + ENTREGA + "\"");
    }

    @Test
    @DisplayName("Cada evento del lote produce su propia fila")
    void escribeUnaFilaPorEvento() {
        List<DomainEvent> eventos = List.of(
                entregaConfirmada(),
                new EntregaFallida(ENTREGA, PacienteId.de("pac-1"), MotivoFallo.AUSENTE, MOMENTO));

        new PublicadorDeEventosOutbox(outbox, objectMapperDeProduccion()).publicar(eventos);

        verify(outbox, times(2)).save(any(OutboxJpaEntity.class));
    }

    @Test
    @DisplayName("Si el evento no se puede serializar, la transaccion se corta: nada a medias")
    void fallaSiElEventoNoSePuedeSerializar(@Mock ObjectMapper objectMapperRoto)
            throws JsonProcessingException {
        when(objectMapperRoto.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("falla simulada") {
                });
        PublicadorDeEventosOutbox publicador = new PublicadorDeEventosOutbox(outbox, objectMapperRoto);
        List<DomainEvent> eventos = List.of(entregaConfirmada());

        assertThatThrownBy(() -> publicador.publicar(eventos))
                .isInstanceOf(IllegalStateException.class);
    }
}
