package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.EntregaJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.persistence.mapper.EntregaJpaMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaRepositoryJpaTest {

    private static final RutaId RUTA = new RutaId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    @Mock
    EntregaJpaRepository jpa;

    private final EntregaJpaMapper mapper = new EntregaJpaMapper();

    private EntregaRepositoryJpa repositorio() {
        return new EntregaRepositoryJpa(jpa, mapper);
    }

    private Entrega entrega(String paquete) {
        return Entrega.programar(PaqueteId.de(paquete), PacienteId.de("pac-1"), RUTA, 2);
    }

    @Test
    @DisplayName("HU-1: una entrega nueva se da de alta como una entidad nueva con su id")
    void unaEntregaNuevaSeDaDeAlta() {
        Entrega entrega = entrega("pkg-1");
        when(jpa.findById(entrega.id().valor())).thenReturn(Optional.empty());

        repositorio().guardar(entrega);

        verify(jpa).save(argThat(e -> e.getId().equals(entrega.id().valor())));
    }

    @Test
    @DisplayName("HU-4: una entrega existente se guarda sobre la entidad administrada, que conserva su version")
    void unaEntregaExistenteSeGuardaSobreLaEntidadAdministrada() {
        Entrega entrega = entrega("pkg-2");
        EntregaJpaEntity administrada = mapper.toNewEntity(entrega);
        when(jpa.findById(entrega.id().valor())).thenReturn(Optional.of(administrada));

        repositorio().guardar(entrega);

        verify(jpa).save(administrada);
    }

    @Test
    @DisplayName("HU-4: buscar una entrega que no existe devuelve vacio")
    void buscarUnaEntregaInexistenteDevuelveVacio() {
        EntregaId id = EntregaId.nuevo();
        when(jpa.findById(id.valor())).thenReturn(Optional.empty());

        assertThat(repositorio().buscarPorId(id)).isEmpty();
    }

    @Test
    @DisplayName("HU-4: buscar una entrega existente la rehidrata con su identidad")
    void buscarUnaEntregaExistenteLaRehidrata() {
        Entrega entrega = entrega("pkg-3");
        when(jpa.findById(entrega.id().valor())).thenReturn(Optional.of(mapper.toNewEntity(entrega)));

        Optional<Entrega> encontrada = repositorio().buscarPorId(entrega.id());

        assertThat(encontrada).map(Entrega::id).contains(entrega.id());
    }

    @Test
    @DisplayName("HU-1: las entregas de una ruta se rehidratan todas")
    void lasEntregasDeUnaRutaSeRehidratan() {
        when(jpa.findByRutaId(RUTA.valor())).thenReturn(List.of(
                mapper.toNewEntity(entrega("pkg-4")), mapper.toNewEntity(entrega("pkg-5"))));

        List<Entrega> entregas = repositorio().buscarPorRuta(RUTA);

        assertThat(entregas).extracting(e -> e.paqueteId().valor()).containsExactly("pkg-4", "pkg-5");
    }
}
