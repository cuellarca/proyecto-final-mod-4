package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaConfirmada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaFallida;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaNoConcretada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaReprogramada;
import com.nurtricenter.logisticdelivery.domain.service.PoliticaDeReintento;
import com.nurtricenter.logisticdelivery.domain.shared.AggregateRoot;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado raiz {@code Entrega}: el acto de entregar un paquete a un paciente y su
 * evidencia. Es la unidad de consistencia de la operacion; aqui viven las reglas de
 * "entregado con constancia" (HU-4) y de "fallo con motivo / reintento" (HU-5).
 *
 * <p>Se relaciona con {@code RutaDeEntrega} <b>por identidad</b> ({@code rutaId}),
 * nunca por referencia directa de objetos (regla de limites de agregado).</p>
 *
 * <p>Invariantes protegidas por la raiz:</p>
 * <ul>
 *   <li>No pasa a ENTREGADA sin una {@code ConstanciaDeEntrega} completa (HU-4).</li>
 *   <li>Una entrega FALLIDA exige {@code motivoFallo} (HU-5).</li>
 *   <li>Un estado terminal (ENTREGADA / NO_CONCRETADA) no admite mas cambios.</li>
 * </ul>
 */
public class Entrega extends AggregateRoot<EntregaId> {

    private final EntregaId entregaId;
    private final PaqueteId paqueteId;
    private final PacienteId pacienteId;
    private final RutaId rutaId;

    private EstadoEntrega estado;
    private Intentos intentos;
    private MotivoFallo motivoFallo;            // presente solo cuando estado = FALLIDA
    private ConstanciaDeEntrega constancia;     // presente solo cuando estado = ENTREGADA

    private Entrega(EntregaId entregaId, PaqueteId paqueteId, PacienteId pacienteId,
                    RutaId rutaId, Intentos intentos) {
        this.entregaId = Objects.requireNonNull(entregaId, "EntregaId requerido");
        this.paqueteId = Objects.requireNonNull(paqueteId, "PaqueteId requerido");
        this.pacienteId = Objects.requireNonNull(pacienteId, "PacienteId requerido");
        this.rutaId = Objects.requireNonNull(rutaId, "RutaId requerido");
        this.intentos = Objects.requireNonNull(intentos, "Intentos requerido");
        this.estado = EstadoEntrega.PENDIENTE;
    }

    /** Programa una entrega pendiente para un paquete dentro de una ruta. */
    public static Entrega programar(PaqueteId paqueteId, PacienteId pacienteId,
                                    RutaId rutaId, int maximoReintentos) {
        return new Entrega(EntregaId.nuevo(), paqueteId, pacienteId, rutaId,
                Intentos.inicial(maximoReintentos));
    }

    /**
     * Reconstruye una {@code Entrega} desde su estado persistido (extension aditiva y pura,
     * usada por la capa de persistencia). No emite eventos ni re-valida invariantes de
     * creacion: asume que el estado provino de un agregado que ya fue valido.
     */
    public static Entrega reconstituir(EntregaId entregaId, PaqueteId paqueteId, PacienteId pacienteId,
                                       RutaId rutaId, EstadoEntrega estado, Intentos intentos,
                                       MotivoFallo motivoFallo, ConstanciaDeEntrega constancia) {
        Entrega entrega = new Entrega(entregaId, paqueteId, pacienteId, rutaId, intentos);
        entrega.estado = Objects.requireNonNull(estado, "Estado requerido para reconstituir");
        entrega.motivoFallo = motivoFallo;
        entrega.constancia = constancia;
        return entrega;
    }

    /**
     * HU-4: confirma la entrega con una constancia completa e inmutable.
     * Invariante central: sin constancia valida no hay entrega.
     */
    public void confirmar(ConstanciaDeEntrega constancia) {
        if (estado.esTerminal()) {
            throw new DomainException("Una entrega " + estado + " no puede confirmarse");
        }
        Objects.requireNonNull(constancia, "La entrega requiere una constancia (HU-4)");
        if (!constancia.estaCompleta()) {
            throw new DomainException("No se puede marcar ENTREGADA sin una constancia completa (HU-4)");
        }
        this.constancia = constancia;
        this.motivoFallo = null;
        this.estado = EstadoEntrega.ENTREGADA;
        registrarEvento(new EntregaConfirmada(entregaId, pacienteId, paqueteId, constancia, constancia.timestamp()));
    }

    /** HU-5: registra un fallo indicando su motivo obligatorio (reloj del sistema). */
    public void registrarFallo(MotivoFallo motivo) {
        registrarFallo(motivo, Instant.now());
    }

    /** HU-5: variante con el instante inyectado desde la aplicacion (tests deterministas, Fase 2). */
    public void registrarFallo(MotivoFallo motivo, Instant ocurridoEn) {
        if (estado.esTerminal()) {
            throw new DomainException("Una entrega " + estado + " no puede registrar fallo");
        }
        Objects.requireNonNull(motivo, "Un fallo exige un motivo (HU-5)");
        Objects.requireNonNull(ocurridoEn, "Instante requerido");
        this.motivoFallo = motivo;
        this.estado = EstadoEntrega.FALLIDA;
        registrarEvento(new EntregaFallida(entregaId, pacienteId, motivo, ocurridoEn));
    }

    /**
     * HU-5: aplica la politica de reintento sobre una entrega fallida (reloj del sistema).
     * Si hay saldo, reprograma (vuelve a PENDIENTE); si no, queda NO_CONCRETADA.
     */
    public void reintentar(PoliticaDeReintento politica) {
        reintentar(politica, Instant.now());
    }

    /** HU-5: variante con el instante inyectado desde la aplicacion (tests deterministas, Fase 2). */
    public void reintentar(PoliticaDeReintento politica, Instant ocurridoEn) {
        if (estado != EstadoEntrega.FALLIDA) {
            throw new DomainException("Solo una entrega FALLIDA puede reintentarse (actual: " + estado + ")");
        }
        Objects.requireNonNull(politica, "Politica de reintento requerida");
        Objects.requireNonNull(ocurridoEn, "Instante requerido");
        switch (politica.decidir(intentos)) {
            case REINTENTAR -> {
                this.intentos = intentos.incrementar();
                this.estado = EstadoEntrega.PENDIENTE;
                registrarEvento(new EntregaReprogramada(entregaId, pacienteId, intentos.valor(), ocurridoEn));
            }
            case NO_CONCRETAR -> {
                this.estado = EstadoEntrega.NO_CONCRETADA;
                registrarEvento(new EntregaNoConcretada(entregaId, pacienteId, ocurridoEn));
            }
        }
    }

    @Override
    public EntregaId id() {
        return entregaId;
    }

    public PaqueteId paqueteId() {
        return paqueteId;
    }

    public PacienteId pacienteId() {
        return pacienteId;
    }

    public RutaId rutaId() {
        return rutaId;
    }

    public EstadoEntrega estado() {
        return estado;
    }

    public Intentos intentos() {
        return intentos;
    }

    public Optional<MotivoFallo> motivoFallo() {
        return Optional.ofNullable(motivoFallo);
    }

    public Optional<ConstanciaDeEntrega> constancia() {
        return Optional.ofNullable(constancia);
    }
}
