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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntregaJpaMapperTest {

    private static final Instant INSTANTE = Instant.parse("2026-07-10T15:30:00Z");
    private static final RutaId RUTA = new RutaId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private final EntregaJpaMapper mapper = new EntregaJpaMapper();

    private Entrega entregaPendiente() {
        return Entrega.programar(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), RUTA, 2);
    }

    private ConstanciaDeEntrega constancia() {
        return new ConstanciaDeEntrega(INSTANTE, new Geolocalizacion(-17.78, -63.18),
                Url.de("https://storage/evidencia.jpg"), "Ana Lopez");
    }

    private Entrega entregaConfirmada() {
        Entrega entrega = entregaPendiente();
        entrega.confirmar(constancia());
        return entrega;
    }

    @Test
    @DisplayName("HU-1: una entrega pendiente se persiste sin constancia ni motivo de fallo")
    void unaEntregaPendienteSePersisteSinConstanciaNiMotivo() {
        EntregaJpaEntity entidad = mapper.toNewEntity(entregaPendiente());

        assertThat(entidad).extracting(EntregaJpaEntity::getEstado, EntregaJpaEntity::getConstanciaUrl,
                        EntregaJpaEntity::getMotivoFallo)
                .containsExactly("PENDIENTE", null, null);
    }

    @Test
    @DisplayName("HU-4: la constancia de una entrega confirmada sobrevive la ida y vuelta a la entidad")
    void laConstanciaSobreviveLaIdaYVuelta() {
        Entrega recuperada = mapper.toDomain(mapper.toNewEntity(entregaConfirmada()));

        assertThat(recuperada.constancia()).contains(constancia());
    }

    @Test
    @DisplayName("HU-4: la entrega confirmada se rehidrata como ENTREGADA")
    void laEntregaConfirmadaSeRehidrataComoEntregada() {
        Entrega recuperada = mapper.toDomain(mapper.toNewEntity(entregaConfirmada()));

        assertThat(recuperada.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
    }

    @Test
    @DisplayName("HU-5: el motivo de una entrega fallida sobrevive la ida y vuelta a la entidad")
    void elMotivoDeFalloSobreviveLaIdaYVuelta() {
        Entrega fallida = entregaPendiente();
        fallida.registrarFallo(MotivoFallo.DIRECCION_ERRONEA, INSTANTE);

        Entrega recuperada = mapper.toDomain(mapper.toNewEntity(fallida));

        assertThat(recuperada.motivoFallo()).contains(MotivoFallo.DIRECCION_ERRONEA);
    }

    @Test
    @DisplayName("HU-5: los intentos consumidos y el maximo se conservan al rehidratar")
    void losIntentosSeConservanAlRehidratar() {
        Entrega conUnReintento = Entrega.reconstituir(EntregaId.nuevo(), PaqueteId.de("pkg-2"),
                PacienteId.de("pac-2"), RUTA, EstadoEntrega.PENDIENTE, new Intentos(1, 2), null, null);

        Entrega recuperada = mapper.toDomain(mapper.toNewEntity(conUnReintento));

        assertThat(recuperada.intentos()).isEqualTo(new Intentos(1, 2));
    }

    @Test
    @DisplayName("HU-4: copiar el estado sobre la entidad administrada deja la entrega ENTREGADA con su evidencia")
    void copiarEstadoLlevaLaConfirmacionALaEntidadAdministrada() {
        Entrega entrega = entregaPendiente();
        EntregaJpaEntity administrada = mapper.toNewEntity(entrega);
        entrega.confirmar(constancia());

        mapper.copiarEstado(administrada, entrega);

        assertThat(administrada).extracting(EntregaJpaEntity::getEstado, EntregaJpaEntity::getConstanciaUrl,
                        EntregaJpaEntity::getConstanciaReceptor)
                .containsExactly("ENTREGADA", "https://storage/evidencia.jpg", "Ana Lopez");
    }

    @Test
    @DisplayName("HU-5: copiar el estado de una entrega sin constancia limpia las columnas de evidencia")
    void copiarEstadoSinConstanciaLimpiaLasColumnasDeEvidencia() {
        EntregaJpaEntity administrada = mapper.toNewEntity(entregaConfirmada());

        mapper.copiarEstado(administrada, entregaPendiente());

        assertThat(administrada).extracting(EntregaJpaEntity::getConstanciaTimestamp,
                        EntregaJpaEntity::getConstanciaLat, EntregaJpaEntity::getConstanciaUrl)
                .containsOnlyNulls();
    }
}
