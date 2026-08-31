package com.nurtricenter.logisticdelivery.domain.ruta;

import com.nurtricenter.logisticdelivery.domain.ruta.event.RutaDeEntregaPlanificada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorDeRutas;
import com.nurtricenter.logisticdelivery.domain.shared.AggregateRoot;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado raiz {@code RutaDeEntrega}: la hoja de ruta de un repartidor para un dia.
 * Es la unidad de consistencia de la planificacion; toda modificacion de sus paradas
 * pasa por aqui.
 *
 * <p>Invariantes protegidas por la raiz:</p>
 * <ul>
 *   <li>El {@code orden} de las paradas es <b>contiguo y unico</b> (1..n).</li>
 *   <li>Toda {@code Parada} tiene {@code Geolocalizacion} (garantizada al construir la parada, HU-2).</li>
 *   <li>Las paradas solo se operan con la ruta EN_CURSO.</li>
 * </ul>
 */
public class RutaDeEntrega extends AggregateRoot<RutaId> {

    private final RutaId rutaId;
    private final RepartidorId repartidorId;
    private final Fecha fecha;
    private EstadoRuta estado;
    private final List<Parada> paradas;

    private RutaDeEntrega(RutaId rutaId, RepartidorId repartidorId, Fecha fecha, List<Parada> paradas) {
        this.rutaId = Objects.requireNonNull(rutaId, "RutaId requerido");
        this.repartidorId = Objects.requireNonNull(repartidorId, "RepartidorId requerido");
        this.fecha = Objects.requireNonNull(fecha, "Fecha requerida");
        Objects.requireNonNull(paradas, "La ruta requiere una lista de paradas");
        if (paradas.isEmpty()) {
            throw new DomainException("Una ruta debe planificarse con al menos una parada");
        }
        validarOrdenContiguoYUnico(paradas);
        this.paradas = new ArrayList<>(paradas);
        this.estado = EstadoRuta.PLANIFICADA;
    }

    // Constructor de reconstitucion: reutiliza la validacion de invariantes pero permite fijar el estado.
    private RutaDeEntrega(RutaId rutaId, RepartidorId repartidorId, Fecha fecha,
                          List<Parada> paradas, EstadoRuta estado) {
        this(rutaId, repartidorId, fecha, paradas);
        this.estado = Objects.requireNonNull(estado, "EstadoRuta requerido");
    }

    /**
     * Crea y planifica una ruta a partir de sus paradas geolocalizadas (HU-1/HU-3, reloj del sistema).
     * Emite el evento {@code RutaDeEntregaPlanificada}.
     */
    public static RutaDeEntrega planificar(RepartidorId repartidorId, Fecha fecha, List<Parada> paradas) {
        return planificar(repartidorId, fecha, paradas, Instant.now());
    }

    /** HU-1/HU-3: variante con el instante inyectado desde la aplicacion (tests deterministas, Fase 2). */
    public static RutaDeEntrega planificar(RepartidorId repartidorId, Fecha fecha,
                                           List<Parada> paradas, Instant ocurridoEn) {
        Objects.requireNonNull(ocurridoEn, "Instante requerido");
        RutaDeEntrega ruta = new RutaDeEntrega(RutaId.nuevo(), repartidorId, fecha, paradas);
        ruta.registrarEvento(new RutaDeEntregaPlanificada(
                ruta.rutaId, repartidorId, fecha, ruta.paradas.size(), ocurridoEn));
        return ruta;
    }

    /**
     * Reconstruye una {@code RutaDeEntrega} desde su estado persistido (extension aditiva y pura,
     * usada por la capa de persistencia). No emite eventos; asume paradas ya validas (orden 1..n).
     */
    public static RutaDeEntrega reconstituir(RutaId rutaId, RepartidorId repartidorId, Fecha fecha,
                                             EstadoRuta estado, List<Parada> paradas) {
        return new RutaDeEntrega(rutaId, repartidorId, fecha, paradas, estado);
    }

    // Invariante: el orden de las paradas debe ser exactamente {1..n}, sin huecos ni repetidos.
    private static void validarOrdenContiguoYUnico(List<Parada> paradas) {
        Set<Integer> ordenes = new HashSet<>();
        for (Parada parada : paradas) {
            if (!ordenes.add(parada.orden())) {
                throw new DomainException("Orden de parada duplicado: " + parada.orden());
            }
        }
        for (int i = 1; i <= paradas.size(); i++) {
            if (!ordenes.contains(i)) {
                throw new DomainException(
                        "El orden de las paradas debe ser contiguo 1.." + paradas.size() + " (falta " + i + ")");
            }
        }
    }

    /** Inicia el recorrido de la ruta (PLANIFICADA -> EN_CURSO). */
    public void iniciar() {
        if (estado != EstadoRuta.PLANIFICADA) {
            throw new DomainException("Solo una ruta PLANIFICADA puede iniciarse (actual: " + estado + ")");
        }
        estado = EstadoRuta.EN_CURSO;
    }

    /** Registra la llegada del repartidor a una parada (PENDIENTE -> EN_CURSO). */
    public void avanzarParada(ParadaId paradaId) {
        exigirRutaEnCurso();
        buscarParada(paradaId).iniciar();
    }

    /** Marca una parada como completada; finaliza la ruta si todas son terminales. */
    public void completarParada(ParadaId paradaId) {
        exigirRutaEnCurso();
        buscarParada(paradaId).completar();
        finalizarSiTodasTerminales();
    }

    /** Marca una parada como fallida; finaliza la ruta si todas son terminales. */
    public void fallarParada(ParadaId paradaId) {
        exigirRutaEnCurso();
        buscarParada(paradaId).fallar();
        finalizarSiTodasTerminales();
    }

    /**
     * Reordena las paradas segun la secuencia optima que devuelve el
     * {@code OptimizadorDeRutas} y reasigna el orden 1..n (HU-3). Solo aplicable
     * mientras la ruta esta PLANIFICADA.
     */
    public void aplicarOptimizacion(OptimizadorDeRutas optimizador, Geolocalizacion origen) {
        Objects.requireNonNull(optimizador, "Optimizador requerido");
        if (estado != EstadoRuta.PLANIFICADA) {
            throw new DomainException("La ruta solo puede optimizarse mientras esta PLANIFICADA (actual: " + estado + ")");
        }
        List<Parada> secuencia = optimizador.optimizar(origen, List.copyOf(paradas));
        if (secuencia.size() != paradas.size() || !new HashSet<>(secuencia).containsAll(paradas)) {
            throw new DomainException("La optimizacion debe devolver exactamente las mismas paradas de la ruta");
        }
        int orden = 1;
        for (Parada parada : secuencia) {
            parada.asignarOrden(orden++);
        }
        paradas.clear();
        paradas.addAll(secuencia);
    }

    private void exigirRutaEnCurso() {
        if (estado != EstadoRuta.EN_CURSO) {
            throw new DomainException("La ruta debe estar EN_CURSO para operar sus paradas (actual: " + estado + ")");
        }
    }

    private Parada buscarParada(ParadaId paradaId) {
        return paradas.stream()
                .filter(parada -> parada.id().equals(paradaId))
                .findFirst()
                .orElseThrow(() -> new DomainException("La parada no pertenece a esta ruta: " + paradaId));
    }

    private void finalizarSiTodasTerminales() {
        if (paradas.stream().allMatch(parada -> parada.estado().esTerminal())) {
            estado = EstadoRuta.FINALIZADA;
        }
    }

    @Override
    public RutaId id() {
        return rutaId;
    }

    public RepartidorId repartidorId() {
        return repartidorId;
    }

    public Fecha fecha() {
        return fecha;
    }

    public EstadoRuta estado() {
        return estado;
    }

    /** Paradas de la ruta ordenadas por su secuencia (copia de solo lectura). */
    public List<Parada> paradas() {
        return paradas.stream()
                .sorted(Comparator.comparingInt(Parada::orden))
                .toList();
    }
}
