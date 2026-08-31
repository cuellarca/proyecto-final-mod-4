package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.query.ConstanciaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarHistorialDeEntregasPorPaciente;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarRutaDelRepartidor;
import com.nurtricenter.logisticdelivery.application.usecase.query.HistorialEntregaView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borde REST de lectura (CQRS, HU-6/HU-1). Interesa el contrato: que el rango de fechas del query
 * string llegue a la consulta, que el JSON exponga lo que el read model devuelve y que la ausencia
 * de constancia sea un 404 y no un 200 con cuerpo vacio.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaControllerTest {

    @Mock
    ConsultarHistorialDeEntregasPorPaciente consultarHistorial;
    @Mock
    ConsultarConstanciaDeEntrega consultarConstancia;
    @Mock
    ConsultarRutaDelRepartidor consultarRutas;

    private MockMvc mockMvc;

    @BeforeEach
    void montarElBorde() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ConsultaController(consultarHistorial, consultarConstancia, consultarRutas))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HU-6: el historial del paciente se expone tal como lo da el read model")
    void elHistorialSeExponeComoJson() throws Exception {
        when(consultarHistorial.ejecutar(anyString(), any(), any())).thenReturn(List.of(
                new HistorialEntregaView("e-1", "pac-1", "pkg-1", "CONFIRMADA", null, 1,
                        Instant.parse("2026-07-10T12:00:00Z"))));

        mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", "pac-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entregaId").value("e-1"))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("HU-6: el rango de fechas del query string llega a la consulta")
    void elRangoDeFechasLlegaALaConsulta() throws Exception {
        mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", "pac-1")
                .param("desde", "2026-07-01")
                .param("hasta", "2026-07-10"));

        verify(consultarHistorial).ejecutar("pac-1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"));
    }

    @Test
    @DisplayName("HU-6: la constancia de una entrega confirmada se expone con su evidencia")
    void laConstanciaSeExponeConSuEvidencia() throws Exception {
        when(consultarConstancia.ejecutar("e-1")).thenReturn(Optional.of(new ConstanciaView(
                "e-1", Instant.parse("2026-07-10T12:00:00Z"), -17.78, -63.18,
                "https://storage/e.jpg", "Ana Lopez")));

        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", "e-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlEvidencia").value("https://storage/e.jpg"));
    }

    @Test
    @DisplayName("HU-6: una entrega sin constancia es 404, no un 200 vacio")
    void sinConstanciaEs404() throws Exception {
        when(consultarConstancia.ejecutar("e-9")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", "e-9"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU-1: la fecha es obligatoria al pedir las rutas de un repartidor")
    void laFechaEsObligatoriaAlPedirRutas() throws Exception {
        mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", "rep-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-1: la fecha pedida llega a la consulta de rutas")
    void laFechaPedidaLlegaALaConsultaDeRutas() throws Exception {
        mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", "rep-1")
                .param("fecha", "2026-07-10"));

        verify(consultarRutas).ejecutar("rep-1", LocalDate.parse("2026-07-10"));
    }
}
