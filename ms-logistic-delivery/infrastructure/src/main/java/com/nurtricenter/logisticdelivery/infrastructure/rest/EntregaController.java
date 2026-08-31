package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallida;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.command.ReintentarEntregaCommand;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.ConfirmarEntregaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RegistrarFalloRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.mapper.RestMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de la entrega: confirmar (HU-4), registrar fallo (HU-5) y reintentar (HU-5).
 * Devuelven 204 No Content porque son comandos sin cuerpo de respuesta.
 */
@RestController
@RequestMapping("/api/v1/entregas")
public class EntregaController {

    private final ConfirmarEntrega confirmarEntrega;
    private final RegistrarEntregaFallida registrarEntregaFallida;
    private final ReintentarEntrega reintentarEntrega;
    private final RestMapper mapper;

    public EntregaController(ConfirmarEntrega confirmarEntrega, RegistrarEntregaFallida registrarEntregaFallida,
                             ReintentarEntrega reintentarEntrega, RestMapper mapper) {
        this.confirmarEntrega = confirmarEntrega;
        this.registrarEntregaFallida = registrarEntregaFallida;
        this.reintentarEntrega = reintentarEntrega;
        this.mapper = mapper;
    }

    @PostMapping("/{entregaId}/confirmacion")
    public ResponseEntity<Void> confirmar(@PathVariable String entregaId,
                                          @Valid @RequestBody ConfirmarEntregaRequest request) {
        confirmarEntrega.ejecutar(mapper.toCommand(entregaId, request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entregaId}/fallo")
    public ResponseEntity<Void> registrarFallo(@PathVariable String entregaId,
                                               @Valid @RequestBody RegistrarFalloRequest request) {
        registrarEntregaFallida.ejecutar(mapper.toCommand(entregaId, request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entregaId}/reintento")
    public ResponseEntity<Void> reintentar(@PathVariable String entregaId) {
        reintentarEntrega.ejecutar(new ReintentarEntregaCommand(entregaId));
        return ResponseEntity.noContent().build();
    }
}
