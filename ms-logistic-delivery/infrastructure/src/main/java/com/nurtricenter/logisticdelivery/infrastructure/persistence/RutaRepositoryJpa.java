package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper.RutaJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA/PostGIS del puerto {@code RutaRepository}. Al guardar aplica load-merge:
 * carga la entidad administrada si existe y copia el estado del agregado sobre ella, para
 * preservar el {@code @Version} (optimistic locking).
 */
@Repository
public class RutaRepositoryJpa implements RutaRepository {

    private final RutaJpaRepository jpa;
    private final RutaJpaMapper mapper;

    public RutaRepositoryJpa(RutaJpaRepository jpa, RutaJpaMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void guardar(RutaDeEntrega ruta) {
        jpa.findById(ruta.id().valor())
                .ifPresentOrElse(
                        entity -> {
                            mapper.copiarEstado(entity, ruta);
                            jpa.save(entity);
                        },
                        () -> jpa.save(mapper.toNewEntity(ruta)));
    }

    @Override
    public Optional<RutaDeEntrega> buscarPorId(RutaId id) {
        return jpa.findById(id.valor()).map(mapper::toDomain);
    }

    @Override
    public List<RutaDeEntrega> buscarPorRepartidorYFecha(RepartidorId repartidorId, Fecha fecha) {
        return jpa.findByRepartidorIdAndFecha(repartidorId.valor(), fecha.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
