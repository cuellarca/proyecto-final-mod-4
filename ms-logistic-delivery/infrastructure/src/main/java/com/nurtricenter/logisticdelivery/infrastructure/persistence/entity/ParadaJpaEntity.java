package com.nurtricenter.logisticdelivery.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

/**
 * Entidad JPA de la {@code Parada} (hija del agregado ruta). La coordenada se guarda como
 * {@code geometry(Point,4326)} de PostGIS mediante Hibernate Spatial + JTS.
 */
@Entity
@Table(name = "parada")
public class ParadaJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private RutaJpaEntity ruta;

    @Column(name = "paquete_id", nullable = false)
    private String paqueteId;

    @Column(name = "paciente_id", nullable = false)
    private String pacienteId;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point geo;

    @Column(nullable = false, length = 20)
    private String estado;

    protected ParadaJpaEntity() {
        // requerido por JPA
    }

    public ParadaJpaEntity(UUID id, String paqueteId, String pacienteId, int orden, Point geo, String estado) {
        this.id = id;
        this.paqueteId = paqueteId;
        this.pacienteId = pacienteId;
        this.orden = orden;
        this.geo = geo;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RutaJpaEntity getRuta() {
        return ruta;
    }

    public void setRuta(RutaJpaEntity ruta) {
        this.ruta = ruta;
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

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public Point getGeo() {
        return geo;
    }

    public void setGeo(Point geo) {
        this.geo = geo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
