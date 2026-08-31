package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.Url;

import java.time.Instant;
import java.util.Objects;

/**
 * Evidencia inmutable de una entrega exitosa (Value Object, HU-4).
 * Se define por su contenido; una vez creada no cambia. El propio constructor
 * garantiza su completitud: no puede existir una constancia invalida.
 *
 * @param timestamp       momento de la entrega
 * @param geolocalizacion donde se entrego
 * @param urlEvidencia    URL de la foto/firma en object storage
 * @param nombreReceptor  quien recibio
 */
public record ConstanciaDeEntrega(
        Instant timestamp,
        Geolocalizacion geolocalizacion,
        Url urlEvidencia,
        String nombreReceptor) {

    public ConstanciaDeEntrega {
        Objects.requireNonNull(timestamp, "La constancia requiere timestamp (HU-4)");
        Objects.requireNonNull(geolocalizacion, "La constancia requiere geolocalizacion (HU-4)");
        Objects.requireNonNull(urlEvidencia, "La constancia requiere foto/firma (HU-4)");
        if (nombreReceptor == null || nombreReceptor.isBlank()) {
            throw new DomainException("La constancia requiere el nombre del receptor (HU-4)");
        }
    }

    /** Verifica la completitud de la evidencia (usada por la raiz antes de confirmar). */
    public boolean estaCompleta() {
        return timestamp != null
                && geolocalizacion != null
                && urlEvidencia != null
                && nombreReceptor != null
                && !nombreReceptor.isBlank();
    }
}
