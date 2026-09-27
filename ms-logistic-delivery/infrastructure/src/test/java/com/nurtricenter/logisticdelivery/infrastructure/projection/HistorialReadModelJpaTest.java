package com.nurtricenter.logisticdelivery.infrastructure.projection;

import com.nurtricenter.logisticdelivery.application.usecase.query.ConstanciaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.HistorialEntregaView;
import com.nurtricenter.logisticdelivery.infrastructure.projection.entity.HistorialEntregaPacienteJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialReadModelJpaTest {

    private static final UUID ENTREGA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant DESDE = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-07-31T23:59:59Z");
    private static final Instant EVENTO = Instant.parse("2026-07-10T12:00:00Z");

    @Mock
    HistorialJpaRepository repositorio;

    private HistorialReadModelJpa readModel() {
        return new HistorialReadModelJpa(repositorio);
    }

    private HistorialEntregaPacienteJpaEntity filaFallida() {
        HistorialEntregaPacienteJpaEntity fila = new HistorialEntregaPacienteJpaEntity(ENTREGA, "pac-1");
        fila.setPaqueteId("pkg-1");
        fila.setEstado("FALLIDA");
        fila.setMotivoFallo("AUSENTE");
        fila.setIntentos(1);
        fila.setUltimoEventoEn(EVENTO);
        return fila;
    }

    private HistorialEntregaPacienteJpaEntity filaConfirmada() {
        HistorialEntregaPacienteJpaEntity fila = new HistorialEntregaPacienteJpaEntity(ENTREGA, "pac-1");
        fila.setEstado("CONFIRMADA");
        fila.setConstanciaTimestamp(EVENTO);
        fila.setConstanciaLat(-17.78);
        fila.setConstanciaLon(-63.18);
        fila.setConstanciaUrl("https://storage/evidencia.jpg");
        fila.setConstanciaReceptor("Ana Lopez");
        return fila;
    }

    @Test
    @DisplayName("HU-6: cada fila del read model del paciente se entrega como vista del historial")
    void elHistorialDelPacienteSeMapeaAVistas() {
        when(repositorio.findByPacienteIdAndUltimoEventoEnBetweenOrderByUltimoEventoEnDesc("pac-1", DESDE, HASTA))
                .thenReturn(List.of(filaFallida()));

        List<HistorialEntregaView> historial = readModel().historialDePaciente("pac-1", DESDE, HASTA);

        assertThat(historial).containsExactly(new HistorialEntregaView(
                "22222222-2222-2222-2222-222222222222", "pac-1", "pkg-1", "FALLIDA", "AUSENTE", 1, EVENTO));
    }

    @Test
    @DisplayName("HU-6: la constancia de una entrega confirmada se sirve desde el read model")
    void laConstanciaDeUnaEntregaConfirmadaSeSirve() {
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.of(filaConfirmada()));

        Optional<ConstanciaView> constancia = readModel().constanciaDe(ENTREGA.toString());

        assertThat(constancia).contains(new ConstanciaView("22222222-2222-2222-2222-222222222222", EVENTO,
                -17.78, -63.18, "https://storage/evidencia.jpg", "Ana Lopez"));
    }

    @Test
    @DisplayName("HU-6: una entrega sin evidencia no tiene constancia que mostrar")
    void unaEntregaSinEvidenciaNoTieneConstancia() {
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.of(filaFallida()));

        assertThat(readModel().constanciaDe(ENTREGA.toString())).isEmpty();
    }

    @Test
    @DisplayName("HU-6: una entrega que no esta en el read model no tiene constancia")
    void unaEntregaDesconocidaNoTieneConstancia() {
        when(repositorio.findById(ENTREGA)).thenReturn(Optional.empty());

        assertThat(readModel().constanciaDe(ENTREGA.toString())).isEmpty();
    }
}
