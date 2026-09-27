package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.RutaJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper.RutaJpaMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RutaRepositoryJpaTest {

    private static final Instant INSTANTE = Instant.parse("2026-07-10T08:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 7, 10);

    @Mock
    RutaJpaRepository jpa;

    private final RutaJpaMapper mapper = new RutaJpaMapper();

    private RutaRepositoryJpa repositorio() {
        return new RutaRepositoryJpa(jpa, mapper);
    }

    private RutaDeEntrega ruta(String repartidor) {
        Parada parada = Parada.crear(PaqueteId.de("pkg-" + repartidor), PacienteId.de("pac-" + repartidor), 1,
                new Geolocalizacion(-17.78, -63.18));
        return RutaDeEntrega.planificar(RepartidorId.de(repartidor), Fecha.de(DIA), List.of(parada), INSTANTE);
    }

    @Test
    @DisplayName("HU-1: una ruta nueva se da de alta como una entidad nueva con sus paradas")
    void unaRutaNuevaSeDaDeAlta() {
        RutaDeEntrega ruta = ruta("rep-1");
        when(jpa.findById(ruta.id().valor())).thenReturn(Optional.empty());

        repositorio().guardar(ruta);

        verify(jpa).save(argThat(e -> e.getId().equals(ruta.id().valor()) && e.getParadas().size() == 1));
    }

    @Test
    @DisplayName("HU-3: una ruta existente se guarda sobre la entidad administrada, que conserva su version")
    void unaRutaExistenteSeGuardaSobreLaEntidadAdministrada() {
        RutaDeEntrega ruta = ruta("rep-2");
        RutaJpaEntity administrada = mapper.toNewEntity(ruta);
        when(jpa.findById(ruta.id().valor())).thenReturn(Optional.of(administrada));

        repositorio().guardar(ruta);

        verify(jpa).save(administrada);
    }

    @Test
    @DisplayName("HU-3: buscar una ruta que no existe devuelve vacio")
    void buscarUnaRutaInexistenteDevuelveVacio() {
        RutaId id = RutaId.nuevo();
        when(jpa.findById(id.valor())).thenReturn(Optional.empty());

        assertThat(repositorio().buscarPorId(id)).isEmpty();
    }

    @Test
    @DisplayName("HU-3: buscar una ruta existente la rehidrata con su identidad")
    void buscarUnaRutaExistenteLaRehidrata() {
        RutaDeEntrega ruta = ruta("rep-3");
        when(jpa.findById(ruta.id().valor())).thenReturn(Optional.of(mapper.toNewEntity(ruta)));

        Optional<RutaDeEntrega> encontrada = repositorio().buscarPorId(ruta.id());

        assertThat(encontrada).map(RutaDeEntrega::id).contains(ruta.id());
    }

    @Test
    @DisplayName("HU-1: las rutas del repartidor para el dia se rehidratan todas")
    void lasRutasDelRepartidorSeRehidratan() {
        RutaDeEntrega ruta = ruta("rep-4");
        when(jpa.findByRepartidorIdAndFecha("rep-4", DIA)).thenReturn(List.of(mapper.toNewEntity(ruta)));

        List<RutaDeEntrega> rutas = repositorio()
                .buscarPorRepartidorYFecha(RepartidorId.de("rep-4"), Fecha.de(DIA));

        assertThat(rutas).extracting(RutaDeEntrega::id).containsExactly(ruta.id());
    }
}
