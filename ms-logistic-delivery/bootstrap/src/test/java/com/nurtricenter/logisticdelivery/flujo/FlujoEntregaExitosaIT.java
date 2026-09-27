package com.nurtricenter.logisticdelivery.flujo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.NotificacionesStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo A, entrega exitosa, de punta a punta: Produccion publica los paquetes, se planifica la ruta,
 * el repartidor la recorre y confirma la entrega con su constancia, ms-notificaciones recibe el
 * evento y el historial del paciente la muestra confirmada.
 */
@Tag("flujo-a")
@PruebaDeIntegracion
class FlujoEntregaExitosaIT {

    private static final String REPARTIDOR = "rep-flujo-a";
    private static final LocalDate FECHA = LocalDate.of(2026, 7, 20);
    private static final String PACIENTE = "pac-flujo-a";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RutaRepository rutaRepository;
    @Autowired
    EntregaRepository entregaRepository;
    @Autowired
    NotificacionesStubListener notificaciones;

    @Test
    @DisplayName("HU-1..HU-6 · flujo correcto: la entrega se planifica, se recorre, se confirma y queda en el historial")
    void laEntregaSePlanificaRecorreConfirmaYQuedaEnElHistorial() throws Exception {
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, RabbitTopologyConfig.RK_PAQUETES_LISTOS,
                new PaquetesListosParaEntregaMessage(REPARTIDOR, FECHA, -17.78, -63.18, List.of(
                        new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-flujo-a", PACIENTE, -17.79, -63.19))));

        await().atMost(Duration.ofSeconds(15)).until(() ->
                !rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de(REPARTIDOR), Fecha.de(FECHA)).isEmpty());
        RutaDeEntrega ruta = rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de(REPARTIDOR), Fecha.de(FECHA)).get(0);
        String rutaId = ruta.id().toString();
        Entrega entrega = entregaRepository.buscarPorRuta(ruta.id()).get(0);
        String entregaId = entrega.id().toString();
        String paradaId = paradaUnica(rutaId);

        mockMvc.perform(post("/api/v1/rutas/{id}/inicio", rutaId)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/rutas/{r}/paradas/{p}/avance", rutaId, paradaId)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/rutas/{r}/paradas/{p}/completar", rutaId, paradaId)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":-17.79,\"lon\":-63.19,\"urlEvidencia\":\"https://s/flujo-a.jpg\",\"nombreReceptor\":\"Pedro\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", REPARTIDOR).param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FINALIZADA"))
                .andExpect(jsonPath("$[0].paradas[0].estado").value("COMPLETADA"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", PACIENTE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[?(@.entregaId=='" + entregaId + "')].estado").value("CONFIRMADA")));
        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", entregaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlEvidencia").value("https://s/flujo-a.jpg"))
                .andExpect(jsonPath("$.nombreReceptor").value("Pedro"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(notificaciones.recibidos()).anySatisfy(e -> {
                    assertThat(e.type()).isEqualTo("EntregaConfirmada");
                    assertThat(e.payload().path("pacienteId").asText()).isEqualTo(PACIENTE);
                }));
    }

    private String paradaUnica(String rutaId) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", REPARTIDOR).param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode rutaNode : json.readTree(cuerpo)) {
            if (rutaNode.get("rutaId").asText().equals(rutaId)) {
                return rutaNode.get("paradas").get(0).get("paradaId").asText();
            }
        }
        throw new IllegalStateException("No se encontro la ruta " + rutaId);
    }
}
