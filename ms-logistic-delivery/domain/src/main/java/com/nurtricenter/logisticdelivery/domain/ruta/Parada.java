package com.nurtricenter.logisticdelivery.domain.ruta;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;

import java.util.Objects;

/**
 * Entidad {@code Parada}: un punto de entrega dentro de una {@code RutaDeEntrega}.
 *
 * <p>Es una Entidad (no un Value Object) porque tiene identidad propia y cambia de
 * estado durante la jornada. Vive <b>dentro</b> del agregado RutaDeEntrega: sus
 * transiciones de estado son de visibilidad de paquete para que solo la raiz las
 * gobierne (nunca se manipula una parada por fuera de su ruta).</p>
 *
 * <p>Invariante HU-2: toda parada tiene {@code Geolocalizacion}; no se puede crear
 * una parada sin coordenadas.</p>
 */
public class Parada {

    private final ParadaId id;
    private final PaqueteId paqueteId;
    private final PacienteId pacienteId;
    private final Geolocalizacion geolocalizacion;
    private int orden;
    private EstadoParada estado;

    private Parada(ParadaId id, PaqueteId paqueteId, PacienteId pacienteId,
                   int orden, Geolocalizacion geolocalizacion, EstadoParada estado) {
        this.id = Objects.requireNonNull(id, "ParadaId requerido");
        this.paqueteId = Objects.requireNonNull(paqueteId, "PaqueteId requerido");
        this.pacienteId = Objects.requireNonNull(pacienteId, "PacienteId requerido");
        this.geolocalizacion = Objects.requireNonNull(geolocalizacion,
                "Una parada debe tener geolocalizacion (HU-2)");
        if (orden < 1) {
            throw new DomainException("El orden de la parada debe ser >= 1");
        }
        this.orden = orden;
        this.estado = Objects.requireNonNull(estado, "EstadoParada requerido");
    }

    /** Crea una parada nueva (estado inicial Pendiente) con identidad generada. */
    public static Parada crear(PaqueteId paqueteId, PacienteId pacienteId,
                               int orden, Geolocalizacion geolocalizacion) {
        return new Parada(ParadaId.nuevo(), paqueteId, pacienteId, orden,
                geolocalizacion, EstadoParada.PENDIENTE);
    }

    /** Reconstruye una parada existente (p. ej. desde persistencia). */
    public static Parada rehidratar(ParadaId id, PaqueteId paqueteId, PacienteId pacienteId,
                                     int orden, Geolocalizacion geolocalizacion, EstadoParada estado) {
        return new Parada(id, paqueteId, pacienteId, orden, geolocalizacion, estado);
    }

    // --- Comportamiento gobernado por la raiz RutaDeEntrega (visibilidad de paquete) ---

    void iniciar() {
        if (estado != EstadoParada.PENDIENTE) {
            throw new DomainException("Solo una parada PENDIENTE puede iniciarse (actual: " + estado + ")");
        }
        estado = EstadoParada.EN_CURSO;
    }

    void completar() {
        if (estado != EstadoParada.EN_CURSO) {
            throw new DomainException("Solo una parada EN_CURSO puede completarse (actual: " + estado + ")");
        }
        estado = EstadoParada.COMPLETADA;
    }

    void fallar() {
        if (estado.esTerminal()) {
            throw new DomainException("Una parada terminal no puede fallar (actual: " + estado + ")");
        }
        estado = EstadoParada.FALLIDA;
    }

    void asignarOrden(int nuevoOrden) {
        if (nuevoOrden < 1) {
            throw new DomainException("El orden de la parada debe ser >= 1");
        }
        this.orden = nuevoOrden;
    }

    // --- Getters (sin setters: el estado solo cambia por comportamiento) ---

    public ParadaId id() {
        return id;
    }

    public PaqueteId paqueteId() {
        return paqueteId;
    }

    public PacienteId pacienteId() {
        return pacienteId;
    }

    public Geolocalizacion geolocalizacion() {
        return geolocalizacion;
    }

    public int orden() {
        return orden;
    }

    public EstadoParada estado() {
        return estado;
    }

    // Identidad: dos paradas son la misma si comparten ParadaId (regla de Entidad).
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Parada otra)) {
            return false;
        }
        return id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Parada{orden=" + orden + ", paquete=" + paqueteId + ", estado=" + estado + "}";
    }
}
