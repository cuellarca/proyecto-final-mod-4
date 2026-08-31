package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.infrastructure.acl.ProveedorDeMapasNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Traduce las excepciones a respuestas {@code application/problem+json} (RFC 7807). Las invariantes
 * del dominio ({@code DomainException}) se mapean a 422; un agregado inexistente a 404; y la caida
 * del proveedor de mapas a 503. Los errores de validacion de framework los cubre la clase base.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntidadNoEncontradaException.class)
    public ProblemDetail manejarNoEncontrada(EntidadNoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail manejarInvarianteDominio(DomainException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio violada", ex.getMessage(), "domain-rule");
    }

    @ExceptionHandler(ProveedorDeMapasNoDisponibleException.class)
    public ProblemDetail manejarProveedorCaido(ProveedorDeMapasNoDisponibleException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Proveedor de mapas no disponible", ex.getMessage(), "maps-unavailable");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail manejarArgumentoInvalido(IllegalArgumentException ex) {
        // p. ej. UUID mal formado en un path variable.
        return problema(HttpStatus.BAD_REQUEST, "Solicitud invalida", ex.getMessage(), "bad-request");
    }

    private ProblemDetail problema(HttpStatus status, String title, String detail, String slug) {
        log.debug("[{}] {} -> {}", status.value(), title, detail);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://nurtricenter.com/problems/" + slug));
        return pd;
    }
}
