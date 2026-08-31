package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaConfirmada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaFallida;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaNoConcretada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaReprogramada;
import com.nurtricenter.logisticdelivery.domain.service.PoliticaDeReintento;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntregaTest {

    /** Instante fijo: el tiempo entra como literal, no como reloj del sistema (P4). */
    private static final Instant MOMENTO = Instant.parse("2026-07-10T12:00:00Z");

    private ConstanciaDeEntrega constanciaValida() {
        return new ConstanciaDeEntrega(MOMENTO, new Geolocalizacion(-17.78, -63.18),
                Url.de("https://storage/evidencia.jpg"), "Maria Perez");
    }

    private Entrega nueva() {
        return Entrega.programar(PaqueteId.de("PKG-1"), PacienteId.de("PAC-1"), RutaId.nuevo(), 2);
    }

    private boolean contiene(List<DomainEvent> eventos, Class<? extends DomainEvent> tipo) {
        return eventos.stream().anyMatch(tipo::isInstance);
    }

    @Test
    @DisplayName("HU-4: confirmar con constancia completa deja ENTREGADA y emite evento")
    void confirmarMarcaEntregadaYEmiteEvento() {
        Entrega entrega = nueva();
        entrega.confirmar(constanciaValida());
        assertEquals(EstadoEntrega.ENTREGADA, entrega.estado());
        assertTrue(entrega.constancia().isPresent());
        assertTrue(contiene(entrega.pullDomainEvents(), EntregaConfirmada.class));
    }

    @Test
    @DisplayName("HU-4: una entrega ya entregada no puede confirmarse de nuevo")
    void noPuedeConfirmarseDosVeces() {
        Entrega entrega = nueva();
        entrega.confirmar(constanciaValida());
        assertThrows(DomainException.class, () -> entrega.confirmar(constanciaValida()));
    }

    @Test
    @DisplayName("HU-5: un fallo exige motivo y deja la entrega FALLIDA")
    void falloExigeMotivoYQuedaFallida() {
        Entrega entrega = nueva();
        entrega.registrarFallo(MotivoFallo.AUSENTE);
        assertEquals(EstadoEntrega.FALLIDA, entrega.estado());
        assertEquals(MotivoFallo.AUSENTE, entrega.motivoFallo().orElseThrow());
        assertTrue(contiene(entrega.pullDomainEvents(), EntregaFallida.class));
    }

    @Test
    @DisplayName("HU-5: fallo sin motivo (null) es rechazado")
    void falloSinMotivoEsRechazado() {
        Entrega entrega = nueva();
        assertThrows(NullPointerException.class, () -> entrega.registrarFallo(null));
    }

    @Test
    @DisplayName("HU-5: con saldo, el reintento reprograma (vuelve a PENDIENTE)")
    void reintentoConSaldoReprograma() {
        Entrega entrega = nueva();
        entrega.registrarFallo(MotivoFallo.AUSENTE);
        entrega.pullDomainEvents();
        entrega.reintentar(new PoliticaDeReintento());
        assertEquals(EstadoEntrega.PENDIENTE, entrega.estado());
        assertEquals(1, entrega.intentos().valor());
        assertTrue(contiene(entrega.pullDomainEvents(), EntregaReprogramada.class));
    }

    @Test
    @DisplayName("HU-5: sin saldo, el reintento deja la entrega NO_CONCRETADA")
    void reintentoSinSaldoNoConcreta() {
        Entrega entrega = Entrega.programar(PaqueteId.de("PKG-1"), PacienteId.de("PAC-1"), RutaId.nuevo(), 1);
        PoliticaDeReintento politica = new PoliticaDeReintento();

        entrega.registrarFallo(MotivoFallo.AUSENTE);
        entrega.reintentar(politica);                 // consume el unico reintento -> PENDIENTE
        entrega.registrarFallo(MotivoFallo.RECHAZADA);
        entrega.pullDomainEvents();
        entrega.reintentar(politica);                 // sin saldo -> NO_CONCRETADA

        assertEquals(EstadoEntrega.NO_CONCRETADA, entrega.estado());
        assertTrue(contiene(entrega.pullDomainEvents(), EntregaNoConcretada.class));
    }

    @Test
    @DisplayName("HU-5: solo una entrega FALLIDA puede reintentarse")
    void soloFallidaPuedeReintentarse() {
        Entrega entrega = nueva();
        assertThrows(DomainException.class, () -> entrega.reintentar(new PoliticaDeReintento()));
    }
}
