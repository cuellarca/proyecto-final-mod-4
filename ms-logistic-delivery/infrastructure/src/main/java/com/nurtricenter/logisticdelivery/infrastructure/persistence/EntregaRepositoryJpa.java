package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper.EntregaJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@code EntregaRepository}. Al guardar aplica load-merge para
 * preservar el {@code @Version} de la entidad (optimistic locking).
 */
@Repository
public class EntregaRepositoryJpa implements EntregaRepository {

    private final EntregaJpaRepository jpa;
    private final EntregaJpaMapper mapper;

    public EntregaRepositoryJpa(EntregaJpaRepository jpa, EntregaJpaMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void guardar(Entrega entrega) {
        jpa.findById(entrega.id().valor())
                .ifPresentOrElse(
                        entity -> {
                            mapper.copiarEstado(entity, entrega);
                            jpa.save(entity);
                        },
                        () -> jpa.save(mapper.toNewEntity(entrega)));
    }

    @Override
    public Optional<Entrega> buscarPorId(EntregaId id) {
        return jpa.findById(id.valor()).map(mapper::toDomain);
    }

    @Override
    public List<Entrega> buscarPorRuta(RutaId rutaId) {
        return jpa.findByRutaId(rutaId.valor()).stream().map(mapper::toDomain).toList();
    }
}
