package com.nurtricenter.logisticdelivery.flujo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 3 (verificacion de integracion contra PostGIS + RabbitMQ externos): los adaptadores de borde
 * REST y AMQP, y el mapeo de errores a {@code application/problem+json}. Requiere Postgres en 5435 y
 * RabbitMQ en 5672.
 */
@EnabledIfSystemProperty(named = "msld.external.db", matches = "true")
@SpringBootTest(classes = LogisticDeliveryApplication.class)
@AutoConfigureMockMvc
class Fase3BordeExternaManualIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RutaRepository rutaRepository;

    @Test
    void restPlanificaConfirmaYMapeaErrores() throws Exception {
        String body = """
                {
                  "repartidorId": "rep-rest-1",
                  "fecha": "2026-07-10",
                  "origenLat": -17.78, "origenLon": -63.18,
                  "paquetes": [ { "paqueteId": "pkg-r1", "pacienteId": "pac-r1", "lat": -17.79, "lon": -63.19 } ]
                }
                """;
        MvcResult creada = mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rutaId").isNotEmpty())
                .andExpect(jsonPath("$.entregas[0].entregaId").isNotEmpty())
                .andReturn();

        String entregaId = json.readTree(creada.getResponse().getContentAsString())
                .get("entregas").get(0).get("entregaId").asText();

        // HU-4: confirmar -> 204
        String confirmacion = """
                { "lat": -17.79, "lon": -63.19, "urlEvidencia": "https://s/e.jpg", "nombreReceptor": "Ana" }
                """;
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON).content(confirmacion))
                .andExpect(status().isNoContent());

        // Confirmar de nuevo una entrega ya terminal -> 422 problem+json (invariante de dominio)
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON).content(confirmacion))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regla de negocio violada"));

        // Confirmar una entrega inexistente -> 404 problem+json
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(confirmacion))
                .andExpect(status().isNotFound());

        // Validacion de framework (cuerpo invalido) -> 400 problem+json
        mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void geocodingOhsDevuelveCoordenadas() throws Exception {
        mockMvc.perform(post("/api/v1/geocoding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"direccion\": \"Av. Banzer 3er anillo\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").isNumber())
                .andExpect(jsonPath("$.lon").isNumber());
    }

    @Test
    void consumerAmqpPlanificaLaRutaDesdePaquetesListos() {
        RepartidorId repartidor = RepartidorId.de("rep-amqp-1");
        LocalDate fecha = LocalDate.of(2026, 7, 12);
        PaquetesListosParaEntregaMessage mensaje = new PaquetesListosParaEntregaMessage(
                repartidor.valor(), fecha, -17.78, -63.18,
                List.of(new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-a1", "pac-a1", -17.79, -63.19)));

        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, RabbitTopologyConfig.RK_PAQUETES_LISTOS, mensaje);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rutaRepository.buscarPorRepartidorYFecha(repartidor, Fecha.de(fecha))).isNotEmpty());
    }
}
