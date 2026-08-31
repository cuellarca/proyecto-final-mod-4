package com.nurtricenter.logisticdelivery.infrastructure.projection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila del read model {@code historial_entrega_paciente} (CQRS lectura, HU-6). Desnormalizada y
 * poblada por el {@code ProyectorHistorial}; las consultas leen solo de aqui.
 */
@Entity
@Table(name = "historial_entrega_paciente")
public class HistorialEntregaPacienteJpaEntity {

    @Id
    @Column(name = "entrega_id")
    private UUID entregaId;

    @Column(name = "paciente_id", nullable = false)
    private String pacienteId;

    @Column(name = "paquete_id")
    private String paqueteId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "motivo_fallo", length = 30)
    private String motivoFallo;

    private Integer intentos;

    @Column(name = "constancia_timestamp")
    private Instant constanciaTimestamp;

    @Column(name = "constancia_lat")
    private Double constanciaLat;

    @Column(name = "constancia_lon")
    private Double constanciaLon;

    @Column(name = "constancia_url", length = 500)
    private String constanciaUrl;

    @Column(name = "constancia_receptor", length = 200)
    private String constanciaReceptor;

    @Column(name = "ultimo_evento_en", nullable = false)
    private Instant ultimoEventoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    protected HistorialEntregaPacienteJpaEntity() {
        // requerido por JPA
    }

    public HistorialEntregaPacienteJpaEntity(UUID entregaId, String pacienteId) {
        this.entregaId = entregaId;
        this.pacienteId = pacienteId;
    }

    public UUID getEntregaId() {
        return entregaId;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public Instant getConstanciaTimestamp() {
        return constanciaTimestamp;
    }

    public void setConstanciaTimestamp(Instant constanciaTimestamp) {
        this.constanciaTimestamp = constanciaTimestamp;
    }

    public Double getConstanciaLat() {
        return constanciaLat;
    }

    public void setConstanciaLat(Double constanciaLat) {
        this.constanciaLat = constanciaLat;
    }

    public Double getConstanciaLon() {
        return constanciaLon;
    }

    public void setConstanciaLon(Double constanciaLon) {
        this.constanciaLon = constanciaLon;
    }

    public String getConstanciaUrl() {
        return constanciaUrl;
    }

    public void setConstanciaUrl(String constanciaUrl) {
        this.constanciaUrl = constanciaUrl;
    }

    public String getConstanciaReceptor() {
        return constanciaReceptor;
    }

    public void setConstanciaReceptor(String constanciaReceptor) {
        this.constanciaReceptor = constanciaReceptor;
    }

    public Instant getUltimoEventoEn() {
        return ultimoEventoEn;
    }

    public void setUltimoEventoEn(Instant ultimoEventoEn) {
        this.ultimoEventoEn = ultimoEventoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
