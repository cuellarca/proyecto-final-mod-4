package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-3 end-to-end: con el proveedor de mapas "caido" (tasa de fallo 100%), planificar la ruta
 * <b>sigue funcionando</b> porque la optimizacion degrada al fallback por cercania (Haversine).
 * Requiere Postgres en 5435.
 */
@EnabledIfSystemProperty(named = "msld.external.db", matches = "true")
@SpringBootTest(
        classes = LogisticDeliveryApplication.class,
        properties = {
                "logistic.maps.stub-failure-rate=1.0"
        })
@AutoConfigureMockMvc
class Fase3DegradacionExternaManualIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void planificaPorCercaniaAunConElProveedorDeMapasCaido() throws Exception {
        String body = """
                {
                  "repartidorId": "rep-degradacion",
                  "fecha": "2026-07-13",
                  "origenLat": -17.78, "origenLon": -63.18,
                  "paquetes": [
                    { "paqueteId": "pkg-d1", "pacienteId": "pac-d1", "lat": -17.79, "lon": -63.19 },
                    { "paqueteId": "pkg-d2", "pacienteId": "pac-d2", "lat": -17.80, "lon": -63.20 }
                  ]
                }
                """;
        mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rutaId").isNotEmpty())
                .andExpect(jsonPath("$.entregas.length()").value(2));
    }
}
