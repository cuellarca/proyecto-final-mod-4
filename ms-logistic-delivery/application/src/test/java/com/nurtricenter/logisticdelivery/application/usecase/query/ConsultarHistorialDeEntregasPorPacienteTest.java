package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.application.port.out.HistorialReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-6: traduccion del rango de fechas del pedido a los instantes UTC con los que se consulta el
 * read model. El comportamiento interesante son los bordes: el dia "hasta" se incluye completo y
 * un extremo ausente abre el rango.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarHistorialDeEntregasPorPacienteTest {

    @Mock
    HistorialReadModel readModel;

    @Test
    @DisplayName("HU-6: el dia 'hasta' se incluye completo (rango [desde 00:00, hasta+1 00:00])")
    void incluyeElDiaHastaCompleto() {
        ConsultarHistorialDeEntregasPorPaciente query = new ConsultarHistorialDeEntregasPorPaciente(readModel);

        query.ejecutar("pac-1", LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"));

        verify(readModel).historialDePaciente("pac-1",
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-11T00:00:00Z"));
    }

    @Test
    @DisplayName("HU-6: sin fecha 'desde' el rango arranca en el origen del tiempo")
    void sinDesdeElRangoArrancaAbierto() {
        ConsultarHistorialDeEntregasPorPaciente query = new ConsultarHistorialDeEntregasPorPaciente(readModel);

        query.ejecutar("pac-1", null, LocalDate.parse("2026-07-10"));

        verify(readModel).historialDePaciente("pac-1",
                Instant.EPOCH, Instant.parse("2026-07-11T00:00:00Z"));
    }

    @Test
    @DisplayName("HU-6: sin fecha 'hasta' el rango queda abierto hacia el futuro")
    void sinHastaElRangoQuedaAbierto() {
        ConsultarHistorialDeEntregasPorPaciente query = new ConsultarHistorialDeEntregasPorPaciente(readModel);

        query.ejecutar("pac-1", LocalDate.parse("2026-07-01"), null);

        verify(readModel).historialDePaciente("pac-1",
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2999-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("HU-6: devuelve el historial tal como lo entrega el read model")
    void devuelveElHistorialDelReadModel() {
        HistorialEntregaView fila = new HistorialEntregaView("e-1", "pac-1", "pkg-1", "CONFIRMADA",
                null, 1, Instant.parse("2026-07-10T12:00:00Z"));
        when(readModel.historialDePaciente(anyString(), any(), any())).thenReturn(List.of(fila));
        ConsultarHistorialDeEntregasPorPaciente query = new ConsultarHistorialDeEntregasPorPaciente(readModel);

        List<HistorialEntregaView> historial =
                query.ejecutar("pac-1", LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"));

        assertThat(historial).containsExactly(fila);
    }
}
