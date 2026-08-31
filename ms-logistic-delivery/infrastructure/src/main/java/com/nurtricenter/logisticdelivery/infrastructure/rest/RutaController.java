package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.command.EjecucionDeRuta;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.PlanificarRutaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RutaPlanificadaResponse;
import com.nurtricenter.logisticdelivery.infrastructure.rest.mapper.RestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de la ruta del dia (HU-1/HU-3): planificar y ejecutar (iniciar y
 * avanzar/completar/fallar sus paradas).
 */
@RestController
@RequestMapping("/api/v1/rutas")
public class RutaController {

    private final PlanificarRutaDelDia planificarRutaDelDia;
    private final EjecucionDeRuta ejecucionDeRuta;
    private final RestMapper mapper;

    public RutaController(PlanificarRutaDelDia planificarRutaDelDia, EjecucionDeRuta ejecucionDeRuta, RestMapper mapper) {
        this.planificarRutaDelDia = planificarRutaDelDia;
        this.ejecucionDeRuta = ejecucionDeRuta;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RutaPlanificadaResponse> planificar(@Valid @RequestBody PlanificarRutaRequest request) {
        PlanificarRutaResultado resultado = planificarRutaDelDia.ejecutar(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(resultado));
    }

    @PostMapping("/{rutaId}/inicio")
    public ResponseEntity<Void> iniciar(@PathVariable String rutaId) {
        ejecucionDeRuta.iniciar(rutaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{rutaId}/paradas/{paradaId}/avance")
    public ResponseEntity<Void> avanzarParada(@PathVariable String rutaId, @PathVariable String paradaId) {
        ejecucionDeRuta.avanzarParada(rutaId, paradaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{rutaId}/paradas/{paradaId}/completar")
    public ResponseEntity<Void> completarParada(@PathVariable String rutaId, @PathVariable String paradaId) {
        ejecucionDeRuta.completarParada(rutaId, paradaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{rutaId}/paradas/{paradaId}/fallo")
    public ResponseEntity<Void> fallarParada(@PathVariable String rutaId, @PathVariable String paradaId) {
        ejecucionDeRuta.fallarParada(rutaId, paradaId);
        return ResponseEntity.noContent().build();
    }
}
