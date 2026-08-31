package com.nurtricenter.logisticdelivery.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de la raiz {@code Entrega}. POJO de persistencia separado del dominio.
 * Las columnas de constancia (HU-4) y de fallo (HU-5) son nulas segun el estado.
 */
@Entity
@Table(name = "entrega")
public class EntregaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "paquete_id", nullable = false)
    private String paqueteId;

    @Column(name = "paciente_id", nullable = false)
    private String pacienteId;

    @Column(name = "ruta_id", nullable = false)
    private UUID rutaId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "intentos_valor", nullable = false)
    private int intentosValor;

    @Column(name = "intentos_maximo", nullable = false)
    private int intentosMaximo;

    @Column(name = "motivo_fallo", length = 30)
    private String motivoFallo;

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

    @Version
    @Column(nullable = false)
    private long version;

    protected EntregaJpaEntity() {
        // requerido por JPA
    }

    public EntregaJpaEntity(UUID id, String paqueteId, String pacienteId, UUID rutaId, String estado,
                            int intentosValor, int intentosMaximo) {
        this.id = id;
        this.paqueteId = paqueteId;
        this.pacienteId = pacienteId;
        this.rutaId = rutaId;
        this.estado = estado;
        this.intentosValor = intentosValor;
        this.intentosMaximo = intentosMaximo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public UUID getRutaId() {
        return rutaId;
    }

    public void setRutaId(UUID rutaId) {
        this.rutaId = rutaId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getIntentosValor() {
        return intentosValor;
    }

    public void setIntentosValor(int intentosValor) {
        this.intentosValor = intentosValor;
    }

    public int getIntentosMaximo() {
        return intentosMaximo;
    }

    public void setIntentosMaximo(int intentosMaximo) {
        this.intentosMaximo = intentosMaximo;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
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

    public long getVersion() {
        return version;
    }
}
