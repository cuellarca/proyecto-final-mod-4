package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConstanciaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarHistorialDeEntregasPorPaciente;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConsultarRutaDelRepartidor;
import com.nurtricenter.logisticdelivery.application.usecase.query.HistorialEntregaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.RutaView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints REST de lectura (CQRS, HU-6/HU-1): historial de entregas por paciente, constancia de una
 * entrega y rutas de un repartidor. Sirven desde el read model / vistas sin tocar el lado de escritura.
 */
@RestController
@RequestMapping("/api/v1")
public class ConsultaController {

    private final ConsultarHistorialDeEntregasPorPaciente consultarHistorial;
    private final ConsultarConstanciaDeEntrega consultarConstancia;
    private final ConsultarRutaDelRepartidor consultarRutas;

    public ConsultaController(ConsultarHistorialDeEntregasPorPaciente consultarHistorial,
                              ConsultarConstanciaDeEntrega consultarConstancia,
                              ConsultarRutaDelRepartidor consultarRutas) {
        this.consultarHistorial = consultarHistorial;
        this.consultarConstancia = consultarConstancia;
        this.consultarRutas = consultarRutas;
    }

    @GetMapping("/pacientes/{pacienteId}/entregas")
    public List<HistorialEntregaView> historialDePaciente(
            @PathVariable String pacienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarHistorial.ejecutar(pacienteId, desde, hasta);
    }

    @GetMapping("/entregas/{entregaId}/constancia")
    public ConstanciaView constancia(@PathVariable String entregaId) {
        return consultarConstancia.ejecutar(entregaId)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "No hay constancia para la entrega: " + entregaId));
    }

    @GetMapping("/repartidores/{repartidorId}/rutas")
    public List<RutaView> rutasDelRepartidor(
            @PathVariable String repartidorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return consultarRutas.ejecutar(repartidorId, fecha);
    }
}
