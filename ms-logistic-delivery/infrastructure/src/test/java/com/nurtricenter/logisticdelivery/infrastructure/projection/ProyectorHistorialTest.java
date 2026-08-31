package com.nurtricenter.logisticdelivery.infrastructure.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.EventoIntegracion;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RegistroDeIdempotencia;
import com.nurtricenter.logisticdelivery.infrastructure.projection.entity.HistorialEntregaPacienteJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proyector del read model del historial (HU-6, lado de lectura de CQRS). Los seams son el
 * repositorio del read model, el registro de idempotencia y el reloj; el evento de integracion es
 * real (JSON del published language), porque es justo el contrato que se quiere afirmar.
 */
@ExtendWith(MockitoExtension.class)
class ProyectorHistorialTest {

    private static final Instant AHORA = Instant.parse("2026-07-10T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final UUID ENTREGA = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    HistorialJpaRepository repositorio;
    @Mock
    RegistroDeIdempotencia idempotencia;
    @Captor
    ArgumentCaptor<HistorialEntregaPacienteJpaEntity> filaCaptor;

    private EventoIntegracion evento(String tipo, String payloadJson) {
        try {
            JsonNode payload = JSON.readTree(payloadJson);
            return new EventoIntegracion("ev-1", tipo, AHORA, EventoIntegracion.VERSION_ACTUAL, payload);
        } catch (Exception e) {
            throw new IllegalStateException("payload de prueba mal formado", e);
        }
    }

    private EventoIntegracion entregaConfirmada() {
        return evento("EntregaConfirmada", """
                {"entregaId":"%s","pacienteId":"pac-1","paqueteId":"pkg-1",
                 "constancia":{"timestamp":"2026-07-10T12:00:00Z",
                               "geolocalizacion":{"lat":-17.78,"lon":-63.18},
                               "urlEvidencia":"https://storage/e.jpg","nombreReceptor":"Ana Lopez"}}
                """.formatted(ENTREGA));
    }

    private ProyectorHistorial proyector() {
        return new ProyectorHistorial(repositorio, idempotencia, RELOJ);
    }

    @Test
    @DisplayName("HU-6: una entrega confirmada queda CONFIRMADA en el historial del paciente")
    void proyectaLaEntregaConfirmada() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(true);
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.empty());

        proyector().onEvento(entregaConfirmada());

        verify(repositorio).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getEstado()).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("HU-6: la constancia se copia al read model para servirla sin tocar la escritura")
    void copiaLaConstanciaAlReadModel() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(true);
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.empty());

        proyector().onEvento(entregaConfirmada());

        verify(repositorio).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getConstanciaUrl()).isEqualTo("https://storage/e.jpg");
    }

    @Test
    @DisplayName("HU-6: una entrega fallida guarda su motivo")
    void proyectaElMotivoDeLaEntregaFallida() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(true);
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.empty());
        EventoIntegracion fallida = evento("EntregaFallida", """
                {"entregaId":"%s","pacienteId":"pac-1","motivo":"AUSENTE"}
                """.formatted(ENTREGA));

        proyector().onEvento(fallida);

        verify(repositorio).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getMotivoFallo()).isEqualTo("AUSENTE");
    }

    @Test
    @DisplayName("HU-6: una reprogramacion registra el numero de intento")
    void proyectaElIntentoDeLaReprogramacion() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(true);
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.empty());
        EventoIntegracion reprogramada = evento("EntregaReprogramada", """
                {"entregaId":"%s","pacienteId":"pac-1","intento":2}
                """.formatted(ENTREGA));

        proyector().onEvento(reprogramada);

        verify(repositorio).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getIntentos()).isEqualTo(2);
    }

    @Test
    @DisplayName("HU-6: el mismo evento entregado dos veces se proyecta una sola vez (idempotencia)")
    void noReprocesaUnEventoYaVisto() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(false);

        proyector().onEvento(entregaConfirmada());

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("HU-6: un evento de ruta no toca el historial, que es por entrega y paciente")
    void ignoraLosEventosSinEntrega() {
        when(idempotencia.primeraVez(anyString(), anyString())).thenReturn(true);
        EventoIntegracion rutaPlanificada = evento("RutaDeEntregaPlanificada", """
                {"rutaId":"r-1","repartidorId":"rep-1","cantidadParadas":3}
                """);

        proyector().onEvento(rutaPlanificada);

        verify(repositorio, never()).save(any());
    }
}
