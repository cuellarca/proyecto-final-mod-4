package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorDeRutas;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso HU-1/HU-3: recibe la carga de entregas del dia (paquetes ya geolocalizados),
 * planifica la ruta y la optimiza (puerto {@code OptimizadorDeRutas}), y programa una {@code Entrega}
 * por paquete. Deja el evento {@code RutaDeEntregaPlanificada} en el Outbox, todo en la misma
 * transaccion.
 */
@Service
public class PlanificarRutaDelDia {

    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final OptimizadorDeRutas optimizador;
    private final PublicadorDeEventos publicador;
    private final Clock clock;
    private final int maximoReintentos;

    public PlanificarRutaDelDia(RutaRepository rutaRepository, EntregaRepository entregaRepository,
                                OptimizadorDeRutas optimizador, PublicadorDeEventos publicador, Clock clock,
                                @Value("${logistic.reintentos.maximo:2}") int maximoReintentos) {
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.optimizador = optimizador;
        this.publicador = publicador;
        this.clock = clock;
        this.maximoReintentos = maximoReintentos;
    }

    @Transactional
    public PlanificarRutaResultado ejecutar(PlanificarRutaDelDiaCommand comando) {
        List<Parada> paradas = new ArrayList<>();
        int orden = 1;
        for (PlanificarRutaDelDiaCommand.Paquete p : comando.paquetes()) {
            paradas.add(Parada.crear(
                    PaqueteId.de(p.paqueteId()),
                    PacienteId.de(p.pacienteId()),
                    orden++,
                    new Geolocalizacion(p.lat(), p.lon())));
        }

        RutaDeEntrega ruta = RutaDeEntrega.planificar(
                RepartidorId.de(comando.repartidorId()),
                Fecha.de(comando.fecha()),
                paradas,
                Instant.now(clock));

        // HU-3: reordena por la secuencia optima (ACL de mapas con fallback Haversine).
        ruta.aplicarOptimizacion(optimizador, new Geolocalizacion(comando.origenLat(), comando.origenLon()));

        rutaRepository.guardar(ruta);
        publicador.publicar(ruta.pullDomainEvents());

        // HU-1: se programa una Entrega por paquete recibido.
        List<PlanificarRutaResultado.EntregaProgramada> entregas = new ArrayList<>();
        for (PlanificarRutaDelDiaCommand.Paquete p : comando.paquetes()) {
            Entrega entrega = Entrega.programar(
                    PaqueteId.de(p.paqueteId()), PacienteId.de(p.pacienteId()), ruta.id(), maximoReintentos);
            entregaRepository.guardar(entrega);
            entregas.add(new PlanificarRutaResultado.EntregaProgramada(
                    entrega.id().toString(), p.paqueteId(), p.pacienteId()));
        }

        return new PlanificarRutaResultado(ruta.id().toString(), entregas);
    }
}
