package com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper;

import com.nurtricenter.logisticdelivery.domain.ruta.EstadoRuta;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.GeoSupport;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.ParadaJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.RutaJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RutaJpaMapperTest {

    private static final Instant INSTANTE = Instant.parse("2026-07-10T08:00:00Z");
    private static final Geolocalizacion CENTRO = new Geolocalizacion(-17.78, -63.18);
    private static final Geolocalizacion EQUIPETROL = new Geolocalizacion(-17.76, -63.19);

    private final RutaJpaMapper mapper = new RutaJpaMapper();

    private Parada parada(String sufijo, int orden, Geolocalizacion geo) {
        return Parada.crear(PaqueteId.de("pkg-" + sufijo), PacienteId.de("pac-" + sufijo), orden, geo);
    }

    private RutaDeEntrega rutaConDosParadas() {
        return RutaDeEntrega.planificar(RepartidorId.de("rep-1"), Fecha.de(LocalDate.of(2026, 7, 10)),
                List.of(parada("a", 1, CENTRO), parada("b", 2, EQUIPETROL)), INSTANTE);
    }

    private ParadaJpaEntity paradaEntidad(int orden, Geolocalizacion geo) {
        return new ParadaJpaEntity(UUID.randomUUID(), "pkg-" + orden, "pac-" + orden, orden,
                GeoSupport.toPoint(geo), "PENDIENTE");
    }

    @Test
    @DisplayName("HU-1: la ruta conserva repartidor, fecha y estado en la ida y vuelta a la entidad")
    void laRutaConservaSusDatosEnLaIdaYVuelta() {
        RutaDeEntrega ruta = rutaConDosParadas();

        RutaDeEntrega recuperada = mapper.toDomain(mapper.toNewEntity(ruta));

        assertThat(recuperada).extracting(RutaDeEntrega::id, RutaDeEntrega::repartidorId,
                        RutaDeEntrega::fecha, RutaDeEntrega::estado)
                .containsExactly(ruta.id(), RepartidorId.de("rep-1"), Fecha.de(LocalDate.of(2026, 7, 10)),
                        EstadoRuta.PLANIFICADA);
    }

    @Test
    @DisplayName("HU-1: cada parada conserva su orden y sus coordenadas en la ida y vuelta")
    void cadaParadaConservaOrdenYCoordenadas() {
        RutaDeEntrega recuperada = mapper.toDomain(mapper.toNewEntity(rutaConDosParadas()));

        assertThat(recuperada.paradas())
                .extracting(Parada::orden, Parada::geolocalizacion)
                .containsExactly(tuple(1, CENTRO), tuple(2, EQUIPETROL));
    }

    @Test
    @DisplayName("HU-1: al rehidratar, las paradas quedan ordenadas aunque la base las devuelva desordenadas")
    void alRehidratarLasParadasQuedanOrdenadas() {
        RutaJpaEntity entidad = new RutaJpaEntity(UUID.randomUUID(), "rep-2", LocalDate.of(2026, 7, 11), "PLANIFICADA");
        entidad.agregarParada(paradaEntidad(2, EQUIPETROL));
        entidad.agregarParada(paradaEntidad(1, CENTRO));

        RutaDeEntrega ruta = mapper.toDomain(entidad);

        assertThat(ruta.paradas()).extracting(Parada::orden).containsExactly(1, 2);
    }

    @Test
    @DisplayName("HU-3: copiar el estado lleva la ruta EN_CURSO a la entidad administrada")
    void copiarEstadoLlevaElEstadoDeLaRuta() {
        RutaDeEntrega ruta = rutaConDosParadas();
        RutaJpaEntity administrada = mapper.toNewEntity(ruta);
        ruta.iniciar();

        mapper.copiarEstado(administrada, ruta);

        assertThat(administrada.getEstado()).isEqualTo("EN_CURSO");
    }

    @Test
    @DisplayName("HU-3: copiar el estado lleva el avance de cada parada a su fila")
    void copiarEstadoLlevaElEstadoDeCadaParada() {
        RutaDeEntrega ruta = rutaConDosParadas();
        RutaJpaEntity administrada = mapper.toNewEntity(ruta);
        ruta.iniciar();
        ruta.avanzarParada(ruta.paradas().get(0).id());

        mapper.copiarEstado(administrada, ruta);

        assertThat(administrada.getParadas()).extracting(ParadaJpaEntity::getOrden, ParadaJpaEntity::getEstado)
                .containsExactly(tuple(1, "EN_CURSO"), tuple(2, "PENDIENTE"));
    }

    @Test
    @DisplayName("HU-1: copiar el estado agrega la parada que la entidad administrada todavia no tenia")
    void copiarEstadoAgregaLaParadaQueFaltaba() {
        RutaDeEntrega ruta = rutaConDosParadas();
        RutaJpaEntity administrada = new RutaJpaEntity(ruta.id().valor(), "rep-1", LocalDate.of(2026, 7, 10), "PLANIFICADA");
        Parada primera = ruta.paradas().get(0);
        administrada.agregarParada(new ParadaJpaEntity(primera.id().valor(), "pkg-a", "pac-a", 1,
                GeoSupport.toPoint(CENTRO), "PENDIENTE"));

        mapper.copiarEstado(administrada, ruta);

        assertThat(administrada.getParadas()).extracting(ParadaJpaEntity::getPaqueteId)
                .containsExactly("pkg-a", "pkg-b");
    }
}
