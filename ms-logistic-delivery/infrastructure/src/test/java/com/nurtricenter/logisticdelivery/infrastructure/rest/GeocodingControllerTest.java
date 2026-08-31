package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.query.GeocodificarDireccion;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.infrastructure.acl.ProveedorDeMapasNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OHS de geocodificacion (HU-2): es el contrato que consume otro bounded context, asi que su
 * comportamiento ante la caida del proveedor externo es parte del contrato, no un detalle.
 */
@ExtendWith(MockitoExtension.class)
class GeocodingControllerTest {

    private static final String CUERPO = """
            {"direccion":"Av. Banzer 3er anillo"}
            """;

    @Mock
    GeocodificarDireccion geocodificarDireccion;

    private MockMvc mockMvc;

    @BeforeEach
    void montarElBorde() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GeocodingController(geocodificarDireccion))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HU-2: una direccion resuelta se devuelve como par lat/lon")
    void devuelveLasCoordenadasResueltas() throws Exception {
        when(geocodificarDireccion.ejecutar("Av. Banzer 3er anillo"))
                .thenReturn(new Geolocalizacion(-17.78, -63.18));

        mockMvc.perform(post("/api/v1/geocoding").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(-17.78))
                .andExpect(jsonPath("$.lon").value(-63.18));
    }

    @Test
    @DisplayName("HU-2: el texto de la direccion llega intacto al caso de uso")
    void laDireccionLlegaAlCasoDeUso() throws Exception {
        when(geocodificarDireccion.ejecutar("Av. Banzer 3er anillo"))
                .thenReturn(new Geolocalizacion(-17.78, -63.18));

        mockMvc.perform(post("/api/v1/geocoding").contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        verify(geocodificarDireccion).ejecutar("Av. Banzer 3er anillo");
    }

    @Test
    @DisplayName("HU-3: con el proveedor de mapas caido el OHS responde 503, no 500")
    void proveedorCaidoEs503() throws Exception {
        when(geocodificarDireccion.ejecutar("Av. Banzer 3er anillo"))
                .thenThrow(new ProveedorDeMapasNoDisponibleException("Proveedor de mapas caido"));

        mockMvc.perform(post("/api/v1/geocoding").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("HU-2: una direccion en blanco es 400: no se llama al proveedor externo")
    void direccionEnBlancoEs400() throws Exception {
        mockMvc.perform(post("/api/v1/geocoding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccion":"  "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
