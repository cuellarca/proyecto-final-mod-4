package com.nurtricenter.logisticdelivery.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** DTOs del OHS de geocodificacion {@code POST /api/v1/geocoding} (HU-2). */
public final class GeocodingDtos {

    private GeocodingDtos() {
    }

    public record GeocodingRequest(@NotBlank String direccion) {
    }

    public record GeocodingResponse(double lat, double lon) {
    }
}
