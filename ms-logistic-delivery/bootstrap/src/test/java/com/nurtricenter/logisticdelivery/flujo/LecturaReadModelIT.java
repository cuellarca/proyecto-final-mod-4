package com.nurtricenter.logisticdelivery.flujo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El read model, poblado por
 * el proyector desde los eventos, responde a las consultas de lectura (HU-6) sin tocar el lado de
 * escritura. Levanta PostGIS y RabbitMQ con Testcontainers.
 */
@PruebaDeIntegracion
class LecturaReadModelIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;

    @Test
    @DisplayName("HU-6 · flujo correcto: el read model se alimenta de los eventos y las consultas responden")
    void confirmarEntregaAlimentaElReadModelYLasConsultasResponden() throws Exception {
        String paciente = "pac-lect-1";
        String repartidor = "rep-lect-1";
        String plan = """
                {
                  "repartidorId": "%s",
                  "fecha": "2026-07-16",
                  "origenLat": -17.78, "origenLon": -63.18,
                  "paquetes": [ { "paqueteId": "pkg-l1", "pacienteId": "%s", "lat": -17.79, "lon": -63.19 } ]
                }
                """.formatted(repartidor, paciente);
        MvcResult creada = mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON).content(plan))
                .andExpect(status().isCreated())
                .andReturn();
        String entregaId = json.readTree(creada.getResponse().getContentAsString())
                .get("entregas").get(0).get("entregaId").asText();

        String urlEvidencia = "https://storage.example.com/evidencia/pkg-l1.jpg";
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":-17.79,\"lon\":-63.19,\"urlEvidencia\":\"" + urlEvidencia
                                + "\",\"nombreReceptor\":\"Lucia\"}"))
                .andExpect(status().isNoContent());

        // El proyector procesa el evento de forma asincrona; se consulta el read model hasta verlo.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", paciente))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[?(@.entregaId=='" + entregaId + "')].estado").value("CONFIRMADA")));

        // La constancia (evidencia) se sirve desde el read model.
        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", entregaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlEvidencia").value(urlEvidencia))
                .andExpect(jsonPath("$.nombreReceptor").value("Lucia"));

        // La ruta del repartidor se consulta (desde el lado de escritura, HU-1).
        mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", repartidor).param("fecha", "2026-07-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paradas[0].pacienteId").value(paciente));
    }

    @Test
    @DisplayName("HU-6 · flujo incorrecto: la constancia de una entrega sin evidencia devuelve 404")
    void constanciaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", java.util.UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }
}
