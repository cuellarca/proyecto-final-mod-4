package com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper;

import com.nurtricenter.logisticdelivery.domain.entrega.ConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.Intentos;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.EntregaJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeo a mano entre el agregado {@code Entrega} y su entidad JPA. Al leer usa
 * {@code Entrega.reconstituir(...)}; la constancia (HU-4) y el motivo de fallo (HU-5)
 * viven en columnas nulas segun el estado.
 */
@Component
public class EntregaJpaMapper {

    /** JPA -> dominio. */
    public Entrega toDomain(EntregaJpaEntity e) {
        Intentos intentos = new Intentos(e.getIntentosValor(), e.getIntentosMaximo());
        MotivoFallo motivo = e.getMotivoFallo() == null ? null : MotivoFallo.valueOf(e.getMotivoFallo());
        ConstanciaDeEntrega constancia = toConstancia(e);

        return Entrega.reconstituir(
                new EntregaId(e.getId()),
                PaqueteId.de(e.getPaqueteId()),
                PacienteId.de(e.getPacienteId()),
                new RutaId(e.getRutaId()),
                EstadoEntrega.valueOf(e.getEstado()),
                intentos,
                motivo,
                constancia);
    }

    private ConstanciaDeEntrega toConstancia(EntregaJpaEntity e) {
        if (e.getConstanciaTimestamp() == null) {
            return null;
        }
        return new ConstanciaDeEntrega(
                e.getConstanciaTimestamp(),
                new Geolocalizacion(e.getConstanciaLat(), e.getConstanciaLon()),
                Url.de(e.getConstanciaUrl()),
                e.getConstanciaReceptor());
    }

    /** Dominio -> nueva entidad JPA (caso de alta). */
    public EntregaJpaEntity toNewEntity(Entrega entrega) {
        EntregaJpaEntity e = new EntregaJpaEntity(
                entrega.id().valor(),
                entrega.paqueteId().valor(),
                entrega.pacienteId().valor(),
                entrega.rutaId().valor(),
                entrega.estado().name(),
                entrega.intentos().valor(),
                entrega.intentos().maximo());
        copiarCamposMutables(e, entrega);
        return e;
    }

    /** Copia el estado del agregado sobre una entidad JPA administrada, preservando su {@code @Version}. */
    public void copiarEstado(EntregaJpaEntity e, Entrega entrega) {
        e.setEstado(entrega.estado().name());
        e.setIntentosValor(entrega.intentos().valor());
        e.setIntentosMaximo(entrega.intentos().maximo());
        copiarCamposMutables(e, entrega);
    }

    private void copiarCamposMutables(EntregaJpaEntity e, Entrega entrega) {
        e.setMotivoFallo(entrega.motivoFallo().map(MotivoFallo::name).orElse(null));

        if (entrega.constancia().isPresent()) {
            ConstanciaDeEntrega c = entrega.constancia().get();
            e.setConstanciaTimestamp(c.timestamp());
            e.setConstanciaLat(c.geolocalizacion().lat());
            e.setConstanciaLon(c.geolocalizacion().lon());
            e.setConstanciaUrl(c.urlEvidencia().valor());
            e.setConstanciaReceptor(c.nombreReceptor());
        } else {
            e.setConstanciaTimestamp(null);
            e.setConstanciaLat(null);
            e.setConstanciaLon(null);
            e.setConstanciaUrl(null);
            e.setConstanciaReceptor(null);
        }
    }
}
