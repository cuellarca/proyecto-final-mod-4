package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntregaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallida;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallidaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntregaCommand;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borde REST de la entrega (HU-4/HU-5). El SUT es el controlador: los casos de uso son el seam y
 * se mockean; el mapper es el real. Lo que se afirma es el contrato HTTP (codigo de estado y
 * traduccion del pedido al comando), no la logica de negocio, que ya tiene sus propias pruebas.
 */
@ExtendWith(MockitoExtension.class)
class EntregaControllerTest {

    private static final String CUERPO_CONFIRMACION = """
            {"lat":-17.78,"lon":-63.18,"urlEvidencia":"https://storage/e.jpg","nombreReceptor":"Ana Lopez"}
            """;

    @Mock
    ConfirmarEntrega confirmarEntrega;
    @Mock
    RegistrarEntregaFallida registrarEntregaFallida;
    @Mock
    ReintentarEntrega reintentarEntrega;

    private MockMvc mockMvc;

    @BeforeEach
    void montarElBorde() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EntregaController(
                        confirmarEntrega, registrarEntregaFallida, reintentarEntrega, new RestMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HU-4: confirmar una entrega responde 204 sin cuerpo")
    void confirmarResponde204() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", "e-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CONFIRMACION))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("HU-4: el id del path y la evidencia del cuerpo llegan juntos al caso de uso")
    void confirmarTraduceElPedidoAlComando() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", "e-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CONFIRMACION));

        verify(confirmarEntrega).ejecutar(new ConfirmarEntregaCommand(
                "e-1", -17.78, -63.18, "https://storage/e.jpg", "Ana Lopez"));
    }

    @Test
    @DisplayName("HU-4: una confirmacion sin receptor no llega al dominio: es 400 del cliente")
    void confirmacionSinReceptorEs400() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", "e-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lat":-17.78,"lon":-63.18,"urlEvidencia":"https://storage/e.jpg"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-4: confirmar una entrega inexistente se traduce a 404")
    void confirmarUnaEntregaInexistenteEs404() throws Exception {
        doThrow(new EntidadNoEncontradaException("No existe la entrega e-9"))
                .when(confirmarEntrega).ejecutar(any());

        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", "e-9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CONFIRMACION))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU-5: el motivo del fallo llega al caso de uso junto al id de la entrega")
    void registrarFalloTraduceElMotivoAlComando() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/fallo", "e-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"motivo":"AUSENTE"}
                        """));

        verify(registrarEntregaFallida).ejecutar(
                new RegistrarEntregaFallidaCommand("e-1", MotivoFallo.AUSENTE));
    }

    @Test
    @DisplayName("HU-5: un fallo sin motivo es 400: el motivo es obligatorio")
    void falloSinMotivoEs400() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/fallo", "e-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-5: reintentar una entrega delega en el caso de uso con el id del path")
    void reintentarDelegaEnElCasoDeUso() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/reintento", "e-1"));

        verify(reintentarEntrega).ejecutar(new ReintentarEntregaCommand("e-1"));
    }
}
