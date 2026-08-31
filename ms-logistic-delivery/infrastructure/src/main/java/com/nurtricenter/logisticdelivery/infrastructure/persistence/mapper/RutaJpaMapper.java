package com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper;

import com.nurtricenter.logisticdelivery.domain.ruta.EstadoParada;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoRuta;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.GeoSupport;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.ParadaJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.RutaJpaEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapeo a mano entre el agregado {@code RutaDeEntrega} y su entidad JPA. Es manual
 * porque los agregados tienen constructores privados y factories con invariantes:
 * al leer se usa {@code RutaDeEntrega.reconstituir(...)} / {@code Parada.rehidratar(...)}.
 */
@Component
public class RutaJpaMapper {

    /** JPA -> dominio (rehidratacion del agregado completo). */
    public RutaDeEntrega toDomain(RutaJpaEntity entity) {
        List<Parada> paradas = entity.getParadas().stream()
                .sorted((a, b) -> Integer.compare(a.getOrden(), b.getOrden()))
                .map(this::toDomainParada)
                .toList();

        return RutaDeEntrega.reconstituir(
                new RutaId(entity.getId()),
                RepartidorId.de(entity.getRepartidorId()),
                Fecha.de(entity.getFecha()),
                EstadoRuta.valueOf(entity.getEstado()),
                paradas);
    }

    private Parada toDomainParada(ParadaJpaEntity p) {
        return Parada.rehidratar(
                new ParadaId(p.getId()),
                PaqueteId.de(p.getPaqueteId()),
                PacienteId.de(p.getPacienteId()),
                p.getOrden(),
                GeoSupport.toGeolocalizacion(p.getGeo()),
                EstadoParada.valueOf(p.getEstado()));
    }

    /** Dominio -> nueva entidad JPA (caso de alta). */
    public RutaJpaEntity toNewEntity(RutaDeEntrega ruta) {
        RutaJpaEntity entity = new RutaJpaEntity(
                ruta.id().valor(),
                ruta.repartidorId().valor(),
                ruta.fecha().valor(),
                ruta.estado().name());
        for (Parada parada : ruta.paradas()) {
            entity.agregarParada(toNewParadaEntity(parada));
        }
        return entity;
    }

    private ParadaJpaEntity toNewParadaEntity(Parada parada) {
        return new ParadaJpaEntity(
                parada.id().valor(),
                parada.paqueteId().valor(),
                parada.pacienteId().valor(),
                parada.orden(),
                GeoSupport.toPoint(parada.geolocalizacion()),
                parada.estado().name());
    }

    /**
     * Copia el estado del agregado sobre una entidad JPA ya administrada (caso de actualizacion),
     * preservando su {@code @Version}. La membresia de paradas es estable tras la planificacion;
     * solo cambian su orden y su estado.
     */
    public void copiarEstado(RutaJpaEntity entity, RutaDeEntrega ruta) {
        entity.setEstado(ruta.estado().name());

        Map<UUID, ParadaJpaEntity> existentes = new LinkedHashMap<>();
        for (ParadaJpaEntity p : entity.getParadas()) {
            existentes.put(p.getId(), p);
        }
        for (Parada parada : ruta.paradas()) {
            ParadaJpaEntity p = existentes.get(parada.id().valor());
            if (p == null) {
                entity.agregarParada(toNewParadaEntity(parada));
            } else {
                p.setOrden(parada.orden());
                p.setEstado(parada.estado().name());
            }
        }
    }
}
