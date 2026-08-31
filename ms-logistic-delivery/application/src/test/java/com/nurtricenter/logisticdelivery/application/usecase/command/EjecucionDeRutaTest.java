package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoParada;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoRuta;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecucionDeRutaTest {

    @Mock
    RutaRepository rutaRepository;
    @Mock
    PublicadorDeEventos publicador;

    private RutaDeEntrega rutaConUnaParada() {
        return RutaDeEntrega.planificar(
                RepartidorId.de("rep-1"), Fecha.de(LocalDate.of(2026, 7, 10)),
                List.of(Parada.crear(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), 1,
                        new Geolocalizacion(-17.78, -63.18))));
    }

    @Test
    void iniciarPoneLaRutaEnCursoYPersiste() {
        RutaDeEntrega ruta = rutaConUnaParada();
        when(rutaRepository.buscarPorId(ruta.id())).thenReturn(Optional.of(ruta));
        EjecucionDeRuta useCase = new EjecucionDeRuta(rutaRepository, publicador);

        useCase.iniciar(ruta.id().toString());

        assertThat(ruta.estado()).isEqualTo(EstadoRuta.EN_CURSO);
        verify(rutaRepository).guardar(ruta);
        verify(publicador).publicar(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void completarLaUnicaParadaFinalizaLaRuta() {
        RutaDeEntrega ruta = rutaConUnaParada();
        ruta.iniciar();
        String paradaId = ruta.paradas().get(0).id().toString();
        when(rutaRepository.buscarPorId(ruta.id())).thenReturn(Optional.of(ruta));
        EjecucionDeRuta useCase = new EjecucionDeRuta(rutaRepository, publicador);

        useCase.avanzarParada(ruta.id().toString(), paradaId);
        useCase.completarParada(ruta.id().toString(), paradaId);

        assertThat(ruta.paradas().get(0).estado()).isEqualTo(EstadoParada.COMPLETADA);
        assertThat(ruta.estado()).isEqualTo(EstadoRuta.FINALIZADA);
    }

    @Test
    void fallaCuandoLaRutaNoExiste() {
        RutaId id = RutaId.nuevo();
        when(rutaRepository.buscarPorId(id)).thenReturn(Optional.empty());
        EjecucionDeRuta useCase = new EjecucionDeRuta(rutaRepository, publicador);

        assertThatThrownBy(() -> useCase.iniciar(id.toString()))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }
}
