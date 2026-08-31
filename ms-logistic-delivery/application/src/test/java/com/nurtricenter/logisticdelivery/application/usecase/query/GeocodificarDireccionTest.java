package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.domain.service.Geocodificador;
import com.nurtricenter.logisticdelivery.domain.shared.Destino;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodificarDireccionTest {

    @Mock
    Geocodificador geocodificador;
    @Captor
    ArgumentCaptor<Destino> destinoCaptor;

    @Test
    void delegaEnElGeocodificadorYDevuelveLasCoordenadas() {
        Geolocalizacion esperada = new Geolocalizacion(-17.7833, -63.1821);
        when(geocodificador.geocodificar(destinoCaptor.capture())).thenReturn(esperada);
        GeocodificarDireccion useCase = new GeocodificarDireccion(geocodificador);

        Geolocalizacion resultado = useCase.ejecutar("Av. Banzer 3er anillo, Santa Cruz");

        assertThat(resultado).isEqualTo(esperada);
        assertThat(destinoCaptor.getValue().texto()).isEqualTo("Av. Banzer 3er anillo, Santa Cruz");
        assertThat(destinoCaptor.getValue().estaGeocodificado()).isFalse();
    }
}
