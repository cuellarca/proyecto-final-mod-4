package com.nurtricenter.logisticdelivery.flujo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RutaPlanificadaResponse;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo incorrecto de punta a punta: el borde REST, el caso de uso y PostGIS reales tienen que
 * rechazar la peticion con el codigo y el {@code application/problem+json} correctos. Los tres
 * escenarios solo se pueden dar con el estado realmente persistido: no se pueden simular con dobles.
 */
@PruebaDeIntegracion
class FlujoIncorrectoIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RutaRepository rutaRepository;

    @Test
    @DisplayName("HU-4 · flujo incorrecto: confirmar una entrega que no existe en la base devuelve 404")
    void confirmarUnaEntregaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lat":-17.79,"lon":-63.19,
                                 "urlEvidencia":"https://s/x.jpg","nombreReceptor":"Ana"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://nurtricenter.com/problems/not-found"));
    }

    @Test
    @DisplayName("HU-4 · flujo incorrecto: confirmar dos veces la misma entrega devuelve 422")
    void confirmarDosVecesLaMismaEntregaDevuelve422() throws Exception {
        String entregaId = entregaProgramada("rep-incorrecto-1", "pkg-incorrecto-1", "pac-incorrecto-1");
        String constancia = """
                {"lat":-17.79,"lon":-63.19,
                 "urlEvidencia":"https://s/ok.jpg","nombreReceptor":"Ana"}""";
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON).content(constancia))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON).content(constancia))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://nurtricenter.com/problems/domain-rule"));
    }

    @Test
    @DisplayName("HU-1 · flujo incorrecto: planificar una ruta sin paquetes devuelve 400")
    void planificarUnaRutaSinPaquetesDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repartidorId":"rep-incorrecto-2","fecha":"2026-07-21",
                                 "origenLat":-17.78,"origenLon":-63.18,"paquetes":[]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-1 · flujo incorrecto: un mensaje AMQP mal formado se descarta y el consumidor sigue vivo")
    void unMensajeAmqpMalFormadoNoTumbaAlConsumidor() {
        LocalDate fecha = LocalDate.of(2026, 7, 22);

        // El converter no puede construir el record: el mensaje se rechaza sin reencolar.
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, RabbitTopologyConfig.RK_PAQUETES_LISTOS,
                "{\"repartidorId\":\"rep-veneno\",\"fecha\":\"no-es-una-fecha\"}");

        // El siguiente mensaje valido se procesa igual: la cola no quedo bloqueada por el anterior.
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, RabbitTopologyConfig.RK_PAQUETES_LISTOS,
                new PaquetesListosParaEntregaMessage("rep-sano", fecha, -17.78, -63.18, List.of(
                        new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-sano", "pac-sano", -17.79, -63.19))));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de("rep-sano"), Fecha.de(fecha)))
                        .hasSize(1));
    }

    private String entregaProgramada(String repartidor, String paquete, String paciente) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repartidorId":"%s","fecha":"2026-07-21",
                                 "origenLat":-17.78,"origenLon":-63.18,
                                 "paquetes":[{"paqueteId":"%s","pacienteId":"%s","lat":-17.79,"lon":-63.19}]}"""
                                .formatted(repartidor, paquete, paciente)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readValue(cuerpo, RutaPlanificadaResponse.class).entregas().get(0).entregaId();
    }
}
