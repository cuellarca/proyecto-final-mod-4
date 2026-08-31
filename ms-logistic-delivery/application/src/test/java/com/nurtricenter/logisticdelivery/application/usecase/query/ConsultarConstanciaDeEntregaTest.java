package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.application.port.out.HistorialReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * HU-6: la consulta de la constancia lee del read model y nada mas. El puerto
 * {@code HistorialReadModel} es el seam (testing-rules.md, LIMITES): se mockea; el resto es real.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarConstanciaDeEntregaTest {

    @Mock
    HistorialReadModel readModel;

    @Test
    @DisplayName("HU-6: devuelve la constancia que expone el read model")
    void devuelveLaConstanciaDelReadModel() {
        ConstanciaView vista = new ConstanciaView("e-1", Instant.parse("2026-07-10T12:00:00Z"),
                -17.78, -63.18, "https://storage/e.jpg", "Ana Lopez");
        when(readModel.constanciaDe("e-1")).thenReturn(Optional.of(vista));
        ConsultarConstanciaDeEntrega query = new ConsultarConstanciaDeEntrega(readModel);

        Optional<ConstanciaView> resultado = query.ejecutar("e-1");

        assertThat(resultado).contains(vista);
    }

    @Test
    @DisplayName("HU-6: una entrega sin constancia devuelve vacio, no una excepcion")
    void devuelveVacioCuandoNoHayConstancia() {
        when(readModel.constanciaDe("e-sin-constancia")).thenReturn(Optional.empty());
        ConsultarConstanciaDeEntrega query = new ConsultarConstanciaDeEntrega(readModel);

        Optional<ConstanciaView> resultado = query.ejecutar("e-sin-constancia");

        assertThat(resultado).isEmpty();
    }
}
