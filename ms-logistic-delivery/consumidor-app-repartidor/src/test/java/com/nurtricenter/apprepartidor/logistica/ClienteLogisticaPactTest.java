package com.nurtricenter.apprepartidor.logistica;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinLike;
import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Contrato de la app del repartidor con {@code ms-logistic-delivery} (lado consumer).
 *
 * <p>Cada {@code @Pact} describe una interaccion; cada {@code @Test} ejercita el {@link ClienteLogistica}
 * real contra el mock server de Pact. Al terminar, Pact escribe
 * {@code pacts/app-repartidor-ms-logistic-delivery.json}, que el provider verifica en
 * {@code ContratoAppRepartidorPactIT}. Reglas: {@code contract-testing-rules.md}.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-logistic-delivery", pactVersion = PactSpecVersion.V3)
class ClienteLogisticaPactTest {

    // Datos que controla el consumidor: van literales en la solicitud y viajan como parametros del
    // estado para que el provider siembre exactamente eso (C5).
    private static final String REPARTIDOR = "rep-pact-app-repartidor";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 5);

    // El id de la entrega lo genera el provider: este valor solo sirve para la corrida del consumer.
    // En la verificacion, Pact lo reemplaza por el que devuelve el @State (C6).
    private static final String ENTREGA_DE_EJEMPLO = "3f0c9a4e-6d1b-4b8e-9c5a-2e7f1d0b8a61";

    @Pact(consumer = "app-repartidor")
    public RequestResponsePact rutasDelDia(PactDslWithProvider builder) {
        return builder
                .given("el repartidor tiene una ruta planificada para la fecha",
                        Map.of("repartidorId", REPARTIDOR, "fecha", FECHA.toString()))
                .uponReceiving("una consulta de las rutas del dia del repartidor")
                    .method("GET")
                    .path("/api/v1/repartidores/" + REPARTIDOR + "/rutas")
                    .query("fecha=" + FECHA)
                .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                    .body(newJsonArrayMinLike(1, rutas -> rutas.object(ruta -> {
                        ruta.uuid("rutaId");
                        ruta.date("fecha", "yyyy-MM-dd", FECHA);
                        ruta.stringMatcher("estado", "PLANIFICADA|EN_CURSO|FINALIZADA", "PLANIFICADA");
                        ruta.minArrayLike("paradas", 1, parada -> {
                            parada.uuid("paradaId");
                            parada.integerType("orden", 1);
                            parada.numberType("lat", -17.79);
                            parada.numberType("lon", -63.19);
                            parada.stringMatcher("estado", "PENDIENTE|EN_CURSO|COMPLETADA|FALLIDA", "PENDIENTE");
                        });
                    })).build())
                .toPact();
    }

    @Pact(consumer = "app-repartidor")
    public RequestResponsePact confirmarEntrega(PactDslWithProvider builder) {
        return builder
                .given("hay una entrega pendiente de confirmar")
                .uponReceiving("la confirmacion de una entrega con su constancia")
                    .method("POST")
                    .pathFromProviderState("/api/v1/entregas/${entregaId}/confirmacion",
                            "/api/v1/entregas/" + ENTREGA_DE_EJEMPLO + "/confirmacion")
                    .headers("Content-Type", "application/json")
                    .body(newJsonBody(constancia -> {
                        constancia.numberValue("lat", -17.79);
                        constancia.numberValue("lon", -63.19);
                        constancia.stringValue("urlEvidencia", "https://evidencias.nurtricenter.com/pact.jpg");
                        constancia.stringValue("nombreReceptor", "Ana Rojas");
                    }).build())
                .willRespondWith()
                    .status(204)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "rutasDelDia")
    @DisplayName("la app obtiene las rutas del dia con sus paradas en orden")
    void laAppObtieneLasRutasDelDia(MockServer mockServer) {
        ClienteLogistica cliente = new ClienteLogistica(mockServer.getUrl());

        List<ClienteLogistica.Ruta> rutas = cliente.rutasDelDia(REPARTIDOR, FECHA);

        // Se afirma la forma que la app necesita para dibujar el recorrido, no cuantas rutas hay ni
        // sus ids: eso lo decide el provider (C4).
        assertThat(rutas).isNotEmpty().allSatisfy(ruta -> {
            assertThat(ruta.rutaId()).isNotBlank();
            assertThat(ruta.fecha()).isNotNull();
            assertThat(ruta.paradas()).isNotEmpty()
                    .allSatisfy(parada -> assertThat(parada.orden()).isPositive());
        });
    }

    @Test
    @PactTestFor(pactMethod = "confirmarEntrega")
    @DisplayName("la app confirma una entrega enviando la constancia")
    void laAppConfirmaUnaEntrega(MockServer mockServer) {
        ClienteLogistica cliente = new ClienteLogistica(mockServer.getUrl());
        ClienteLogistica.Constancia constancia = new ClienteLogistica.Constancia(
                -17.79, -63.19, "https://evidencias.nurtricenter.com/pact.jpg", "Ana Rojas");

        // El contrato promete 204 sin cuerpo: lo unico que la app necesita es que no falle (C4).
        assertThatCode(() -> cliente.confirmarEntrega(ENTREGA_DE_EJEMPLO, constancia))
                .doesNotThrowAnyException();
    }
}
