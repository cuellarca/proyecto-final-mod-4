package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.query.GeocodificarDireccion;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.GeocodingDtos.GeocodingRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.GeocodingDtos.GeocodingResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OHS de geocodificacion (HU-2): expone a Suscripcion la resolucion de una direccion a coordenadas.
 */
@RestController
@RequestMapping("/api/v1/geocoding")
public class GeocodingController {

    private final GeocodificarDireccion geocodificarDireccion;

    public GeocodingController(GeocodificarDireccion geocodificarDireccion) {
        this.geocodificarDireccion = geocodificarDireccion;
    }

    @PostMapping
    public GeocodingResponse geocodificar(@Valid @RequestBody GeocodingRequest request) {
        Geolocalizacion geo = geocodificarDireccion.ejecutar(request.direccion());
        return new GeocodingResponse(geo.lat(), geo.lon());
    }
}
