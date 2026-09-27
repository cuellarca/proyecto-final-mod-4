package com.nurtricenter.apprepartidor.logistica;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP de la app del repartidor hacia {@code ms-logistic-delivery}.
 *
 * <p>Es el codigo que la prueba de contrato ejercita: el pacto describe lo que <b>este</b> cliente
 * manda y lo que necesita leer de la respuesta, nada mas. Por eso los records de abajo solo tienen
 * los campos que la app usa, aunque la API devuelva otros.
 */
public class ClienteLogistica {

    private final RestClient http;

    public ClienteLogistica(String urlBase) {
        this.http = RestClient.create(urlBase);
    }

    /** HU-1: las rutas que el repartidor tiene asignadas para el dia, con sus paradas en orden. */
    public List<Ruta> rutasDelDia(String repartidorId, LocalDate fecha) {
        return http.get()
                .uri("/api/v1/repartidores/{repartidorId}/rutas?fecha={fecha}", repartidorId, fecha)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
    }

    /** HU-4: confirma la entrega con la constancia tomada en la puerta del paciente. */
    public void confirmarEntrega(String entregaId, Constancia constancia) {
        http.post()
                .uri("/api/v1/entregas/{entregaId}/confirmacion", entregaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(constancia)
                .retrieve()
                .toBodilessEntity();
    }

    public record Ruta(String rutaId, LocalDate fecha, String estado, List<Parada> paradas) {
    }

    public record Parada(String paradaId, int orden, double lat, double lon, String estado) {
    }

    public record Constancia(double lat, double lon, String urlEvidencia, String nombreReceptor) {
    }
}
