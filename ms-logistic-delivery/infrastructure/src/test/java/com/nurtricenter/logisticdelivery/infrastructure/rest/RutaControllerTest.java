package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.command.EjecucionDeRuta;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.infrastructure.rest.mapper.RestMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borde REST de la ruta del dia (HU-1/HU-3): planificacion y ejecucion de sus paradas.
 */
@ExtendWith(MockitoExtension.class)
class RutaControllerTest {

    private static final String CUERPO_PLANIFICACION = """
            {"repartidorId":"rep-1","fecha":"2026-07-10","origenLat":-17.7833,"origenLon":-63.1821,
             "paquetes":[{"paqueteId":"pkg-1","pacienteId":"pac-1","lat":-17.78,"lon":-63.18}]}
            """;

    @Mock
    PlanificarRutaDelDia planificarRutaDelDia;
    @Mock
    EjecucionDeRuta ejecucionDeRuta;

    private MockMvc mockMvc;

    @BeforeEach
    void montarElBorde() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RutaController(planificarRutaDelDia, ejecucionDeRuta, new RestMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HU-1: planificar devuelve 201 con la ruta creada y sus entregas")
    void planificarDevuelve201ConLaRutaCreada() throws Exception {
        when(planificarRutaDelDia.ejecutar(any())).thenReturn(new PlanificarRutaResultado("r-1",
                List.of(new PlanificarRutaResultado.EntregaProgramada("e-1", "pkg-1", "pac-1"))));

        mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_PLANIFICACION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rutaId").value("r-1"))
                .andExpect(jsonPath("$.entregas[0].entregaId").value("e-1"));
    }

    @Test
    @DisplayName("HU-1: el cuerpo del pedido llega al caso de uso como comando")
    void planificarTraduceElPedidoAlComando() throws Exception {
        when(planificarRutaDelDia.ejecutar(any())).thenReturn(new PlanificarRutaResultado("r-1", List.of()));

        mockMvc.perform(post("/api/v1/rutas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_PLANIFICACION));

        verify(planificarRutaDelDia).ejecutar(new PlanificarRutaDelDiaCommand(
                "rep-1", LocalDate.parse("2026-07-10"), -17.7833, -63.1821,
                List.of(new PlanificarRutaDelDiaCommand.Paquete("pkg-1", "pac-1", -17.78, -63.18))));
    }

    @Test
    @DisplayName("HU-1: planificar una ruta sin paquetes es 400: no hay nada que entregar")
    void planificarSinPaquetesEs400() throws Exception {
        mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repartidorId":"rep-1","fecha":"2026-07-10","origenLat":-17.78,
                                 "origenLon":-63.18,"paquetes":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-3: iniciar la ruta responde 204")
    void iniciarResponde204() throws Exception {
        mockMvc.perform(post("/api/v1/rutas/{id}/inicio", "r-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("HU-3: avanzar una parada delega con los dos ids del path")
    void avanzarParadaDelegaConLosIdsDelPath() throws Exception {
        mockMvc.perform(post("/api/v1/rutas/{rutaId}/paradas/{paradaId}/avance", "r-1", "p-1"));

        verify(ejecucionDeRuta).avanzarParada("r-1", "p-1");
    }

    @Test
    @DisplayName("HU-3: completar una parada delega con los dos ids del path")
    void completarParadaDelegaConLosIdsDelPath() throws Exception {
        mockMvc.perform(post("/api/v1/rutas/{rutaId}/paradas/{paradaId}/completar", "r-1", "p-1"));

        verify(ejecucionDeRuta).completarParada("r-1", "p-1");
    }

    @Test
    @DisplayName("HU-5: fallar una parada delega con los dos ids del path")
    void fallarParadaDelegaConLosIdsDelPath() throws Exception {
        mockMvc.perform(post("/api/v1/rutas/{rutaId}/paradas/{paradaId}/fallo", "r-1", "p-1"));

        verify(ejecucionDeRuta).fallarParada("r-1", "p-1");
    }
}
