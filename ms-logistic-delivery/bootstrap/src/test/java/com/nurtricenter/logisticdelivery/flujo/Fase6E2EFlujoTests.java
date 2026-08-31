package com.nurtricenter.logisticdelivery.flujo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.CateringStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.NotificacionesStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 6 — logica del flujo end-to-end que encadena las 6 HUs: publicar {@code PaquetesListosParaEntrega}
 * (HU-1, paradas geolocalizadas HU-2, ruta optimizada HU-3) -> ejecutar la ruta -> confirmar una entrega
 * con constancia (HU-4) -> fallar y agotar los reintentos de otra (HU-5) -> el Outbox publica, el proyector
 * actualiza y se consulta el respaldo (HU-6), con los eventos llegando a los consumidores stub. El cableado
 * del contexto (Testcontainers o base externa) lo aporta cada subclase concreta.
 */
abstract class Fase6E2EFlujoTests {

    private static final String REPARTIDOR = "rep-e2e";
    private static final LocalDate FECHA = LocalDate.of(2026, 7, 20);
    private static final String PAC_OK = "pac-e2e-ok";
    private static final String PAC_FAIL = "pac-e2e-fail";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected RabbitTemplate rabbitTemplate;
    @Autowired
    protected RutaRepository rutaRepository;
    @Autowired
    protected EntregaRepository entregaRepository;
    @Autowired
    protected NotificacionesStubListener notificaciones;
    @Autowired
    protected CateringStubListener catering;

    @Test
    void flujoCompletoDeLasSeisHistorias() throws Exception {
        // HU-1: el productor externo (stub) publica los paquetes listos, ya geolocalizados (HU-2).
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, RabbitTopologyConfig.RK_PAQUETES_LISTOS,
                new PaquetesListosParaEntregaMessage(REPARTIDOR, FECHA, -17.78, -63.18, List.of(
                        new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-e2e-ok", PAC_OK, -17.79, -63.19),
                        new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-e2e-fail", PAC_FAIL, -17.80, -63.20))));

        // La ruta se planifica y optimiza (HU-3); esperamos a que exista.
        await().atMost(Duration.ofSeconds(15)).until(() ->
                !rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de(REPARTIDOR), Fecha.de(FECHA)).isEmpty());
        RutaDeEntrega ruta = rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de(REPARTIDOR), Fecha.de(FECHA)).get(0);
        String rutaId = ruta.id().toString();

        // Entregas creadas (una por paquete): mapeamos paquete -> entregaId.
        Map<String, String> entregaPorPaquete = entregaRepository.buscarPorRuta(ruta.id()).stream()
                .collect(Collectors.toMap(e -> e.paqueteId().valor(), e -> e.id().toString()));
        assertThat(entregaPorPaquete).containsKeys("pkg-e2e-ok", "pkg-e2e-fail");

        // HU-3 (ejecucion): iniciar la ruta y avanzar/completar la parada del paquete OK.
        mockMvc.perform(post("/api/v1/rutas/{id}/inicio", rutaId)).andExpect(status().isNoContent());
        String paradaOk = paradaIdDe(rutaId, "pkg-e2e-ok");
        mockMvc.perform(post("/api/v1/rutas/{r}/paradas/{p}/avance", rutaId, paradaOk)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/rutas/{r}/paradas/{p}/completar", rutaId, paradaOk)).andExpect(status().isNoContent());

        // HU-4: confirmar la entrega OK con su constancia.
        String entregaOk = entregaPorPaquete.get("pkg-e2e-ok");
        mockMvc.perform(post("/api/v1/entregas/{id}/confirmacion", entregaOk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":-17.79,\"lon\":-63.19,\"urlEvidencia\":\"https://s/ok.jpg\",\"nombreReceptor\":\"Pedro\"}"))
                .andExpect(status().isNoContent());

        // HU-5: fallar la otra entrega y agotar el unico reintento (maximo=1) -> no concretada.
        String entregaFail = entregaPorPaquete.get("pkg-e2e-fail");
        falloYReintento(entregaFail);
        falloYReintento(entregaFail);

        // HU-6: el respaldo se sirve desde el read model, poblado por los eventos.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", PAC_OK))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[?(@.entregaId=='" + entregaOk + "')].estado").value("CONFIRMADA")));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/pacientes/{id}/entregas", PAC_FAIL))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[?(@.entregaId=='" + entregaFail + "')].estado").value("NO_CONCRETADA")));
        mockMvc.perform(get("/api/v1/entregas/{id}/constancia", entregaOk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlEvidencia").value("https://s/ok.jpg"));

        // Saga: notificaciones (confirmada) y catering (no concretada).
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(notificaciones.recibidos()).anySatisfy(e ->
                    assertThat(e.payload().path("pacienteId").asText()).isEqualTo(PAC_OK));
            assertThat(catering.recibidos()).anySatisfy(e -> {
                assertThat(e.type()).isEqualTo("EntregaNoConcretada");
                assertThat(e.payload().path("pacienteId").asText()).isEqualTo(PAC_FAIL);
            });
        });
    }

    private void falloYReintento(String entregaId) throws Exception {
        mockMvc.perform(post("/api/v1/entregas/{id}/fallo", entregaId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"AUSENTE\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/entregas/{id}/reintento", entregaId)).andExpect(status().isNoContent());
    }

    private String paradaIdDe(String rutaId, String paqueteId) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/v1/repartidores/{id}/rutas", REPARTIDOR)
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode rutaNode : json.readTree(cuerpo)) {
            if (rutaNode.get("rutaId").asText().equals(rutaId)) {
                for (JsonNode parada : rutaNode.get("paradas")) {
                    if (parada.get("paqueteId").asText().equals(paqueteId)) {
                        return parada.get("paradaId").asText();
                    }
                }
            }
        }
        throw new IllegalStateException("No se encontro la parada del paquete " + paqueteId);
    }
}
