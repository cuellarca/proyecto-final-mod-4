package com.nurtricenter.logisticdelivery.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA de la raiz {@code RutaDeEntrega}. Es un POJO de persistencia separado del
 * dominio (sin logica de negocio); el mapeo dominio<->JPA se hace a mano.
 */
@Entity
@Table(name = "ruta")
public class RutaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "repartidor_id", nullable = false)
    private String repartidorId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 20)
    private String estado;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ParadaJpaEntity> paradas = new ArrayList<>();

    protected RutaJpaEntity() {
        // requerido por JPA
    }

    public RutaJpaEntity(UUID id, String repartidorId, LocalDate fecha, String estado) {
        this.id = id;
        this.repartidorId = repartidorId;
        this.fecha = fecha;
        this.estado = estado;
    }

    /** Vincula una parada a esta ruta manteniendo la coherencia de la relacion bidireccional. */
    public void agregarParada(ParadaJpaEntity parada) {
        parada.setRuta(this);
        this.paradas.add(parada);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(String repartidorId) {
        this.repartidorId = repartidorId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getVersion() {
        return version;
    }

    public List<ParadaJpaEntity> getParadas() {
        return paradas;
    }

    public void setParadas(List<ParadaJpaEntity> paradas) {
        this.paradas = paradas;
    }
}
