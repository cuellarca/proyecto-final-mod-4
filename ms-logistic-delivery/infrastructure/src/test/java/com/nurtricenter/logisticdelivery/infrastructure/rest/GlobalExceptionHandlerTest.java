package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.infrastructure.acl.ProveedorDeMapasNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de errores del microservicio: que codigo HTTP ve un cliente ante cada clase de fallo.
 * Es el limite entre "regla de negocio violada" (422), "no existe" (404) y "dependencia caida"
 * (503); confundirlos rompe a los consumidores del API.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Un agregado inexistente es 404, no un error del servidor")
    void entidadNoEncontradaEs404() {
        ProblemDetail problema = handler.manejarNoEncontrada(
                new EntidadNoEncontradaException("No existe la entrega e-1"));

        assertThat(problema.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Una invariante de dominio violada es 422: la peticion se entendio pero no procede")
    void invarianteDeDominioEs422() {
        ProblemDetail problema = handler.manejarInvarianteDominio(
                new DomainException("La entrega ya fue confirmada"));

        assertThat(problema.getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("HU-3: el proveedor de mapas caido es 503, no un 500 del microservicio")
    void proveedorDeMapasCaidoEs503() {
        ProblemDetail problema = handler.manejarProveedorCaido(
                new ProveedorDeMapasNoDisponibleException("Proveedor de mapas caido"));

        assertThat(problema.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("Un identificador mal formado es 400: el error es del cliente")
    void argumentoInvalidoEs400() {
        ProblemDetail problema = handler.manejarArgumentoInvalido(
                new IllegalArgumentException("UUID mal formado: abc"));

        assertThat(problema.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("El cuerpo del error es problem+json con el tipo del problema (RFC 7807)")
    void elProblemaLlevaSuTipoRfc7807() {
        ProblemDetail problema = handler.manejarNoEncontrada(
                new EntidadNoEncontradaException("No existe la entrega e-1"));

        assertThat(problema.getType())
                .hasToString("https://nurtricenter.com/problems/not-found");
    }
}
