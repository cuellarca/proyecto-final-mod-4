package com.nurtricenter.logisticdelivery.persistence;

import com.nurtricenter.logisticdelivery.domain.entrega.ConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoParada;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoRuta;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 1 — logica del round-trip de persistencia sobre PostGIS real: guardar y rehidratar los
 * agregados {@code RutaDeEntrega} (con paradas geolocalizadas) y {@code Entrega}, verificando que
 * el dominio se reconstruye 1:1 sin anotaciones de framework. El cableado del contexto (Testcontainers
 * o base externa) lo aporta cada subclase concreta.
 */
abstract class PersistenciaRoundTripTests {

    @Autowired
    protected RutaRepository rutaRepository;

    @Autowired
    protected EntregaRepository entregaRepository;

    @Test
    void guardaYRehidrataUnaRutaConParadasGeolocalizadas() {
        RepartidorId repartidor = RepartidorId.de("rep-001");
        Fecha fecha = Fecha.de(2026, 7, 10);
        List<Parada> paradas = List.of(
                Parada.crear(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), 1, new Geolocalizacion(-17.7833, -63.1821)),
                Parada.crear(PaqueteId.de("pkg-2"), PacienteId.de("pac-2"), 2, new Geolocalizacion(-17.7900, -63.1900)),
                Parada.crear(PaqueteId.de("pkg-3"), PacienteId.de("pac-3"), 3, new Geolocalizacion(-17.8000, -63.2000)));
        RutaDeEntrega ruta = RutaDeEntrega.planificar(repartidor, fecha, paradas);
        RutaId rutaId = ruta.id();

        rutaRepository.guardar(ruta);

        RutaDeEntrega recuperada = rutaRepository.buscarPorId(rutaId).orElseThrow();
        assertThat(recuperada.repartidorId()).isEqualTo(repartidor);
        assertThat(recuperada.fecha()).isEqualTo(fecha);
        assertThat(recuperada.estado()).isEqualTo(EstadoRuta.PLANIFICADA);
        assertThat(recuperada.paradas()).hasSize(3);
        Parada primera = recuperada.paradas().get(0);
        assertThat(primera.orden()).isEqualTo(1);
        assertThat(primera.paqueteId()).isEqualTo(PaqueteId.de("pkg-1"));
        assertThat(primera.geolocalizacion().lat()).isEqualTo(-17.7833);
        assertThat(primera.geolocalizacion().lon()).isEqualTo(-63.1821);
        assertThat(primera.estado()).isEqualTo(EstadoParada.PENDIENTE);
    }

    @Test
    void actualizaUnaRutaEnCursoPreservandoLaIdentidadDeSusParadas() {
        RutaDeEntrega ruta = RutaDeEntrega.planificar(
                RepartidorId.de("rep-002"), Fecha.de(2026, 7, 11),
                List.of(Parada.crear(PaqueteId.de("pkg-a"), PacienteId.de("pac-a"), 1,
                        new Geolocalizacion(-17.78, -63.18))));
        rutaRepository.guardar(ruta);

        // Avanza el ciclo de vida y vuelve a guardar (camino de actualizacion / load-merge).
        ruta.iniciar();
        ruta.avanzarParada(ruta.paradas().get(0).id());
        ruta.completarParada(ruta.paradas().get(0).id());
        rutaRepository.guardar(ruta);

        RutaDeEntrega recuperada = rutaRepository.buscarPorId(ruta.id()).orElseThrow();
        assertThat(recuperada.estado()).isEqualTo(EstadoRuta.FINALIZADA);
        assertThat(recuperada.paradas().get(0).estado()).isEqualTo(EstadoParada.COMPLETADA);
    }

    @Test
    void guardaYRehidrataUnaEntregaConfirmadaConSuConstancia() {
        Entrega entrega = Entrega.programar(
                PaqueteId.de("pkg-9"), PacienteId.de("pac-9"), RutaId.nuevo(), 2);
        Instant momento = Instant.parse("2026-07-10T14:30:00Z");
        entrega.confirmar(new ConstanciaDeEntrega(
                momento,
                new Geolocalizacion(-17.7833, -63.1821),
                Url.de("https://storage.example.com/evidencia/pkg-9.jpg"),
                "Maria Perez"));
        EntregaId id = entrega.id();

        entregaRepository.guardar(entrega);

        Entrega recuperada = entregaRepository.buscarPorId(id).orElseThrow();
        assertThat(recuperada.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(recuperada.constancia()).isPresent();
        ConstanciaDeEntrega c = recuperada.constancia().orElseThrow();
        assertThat(c.timestamp()).isEqualTo(momento);
        assertThat(c.nombreReceptor()).isEqualTo("Maria Perez");
        assertThat(c.urlEvidencia()).isEqualTo(Url.de("https://storage.example.com/evidencia/pkg-9.jpg"));
        assertThat(c.geolocalizacion().lat()).isEqualTo(-17.7833);
        assertThat(recuperada.motivoFallo()).isEmpty();
    }

    @Test
    void guardaYRehidrataUnaEntregaFallidaConSuMotivo() {
        Entrega entrega = Entrega.programar(
                PaqueteId.de("pkg-8"), PacienteId.de("pac-8"), RutaId.nuevo(), 2);
        entrega.registrarFallo(MotivoFallo.AUSENTE);
        EntregaId id = entrega.id();

        entregaRepository.guardar(entrega);

        Entrega recuperada = entregaRepository.buscarPorId(id).orElseThrow();
        assertThat(recuperada.estado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(recuperada.motivoFallo()).contains(MotivoFallo.AUSENTE);
        assertThat(recuperada.constancia()).isEmpty();
    }
}
