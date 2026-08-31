package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Query (HU-1): rutas de un repartidor en un dia. Se lee del lado de escritura (agregado ruta),
 * que es su fuente de verdad, y se proyecta a una vista de lectura.
 */
@Service
public class ConsultarRutaDelRepartidor {

    private final RutaRepository rutaRepository;

    public ConsultarRutaDelRepartidor(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public List<RutaView> ejecutar(String repartidorId, LocalDate fecha) {
        return rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de(repartidorId), Fecha.de(fecha)).stream()
                .map(this::toView)
                .toList();
    }

    private RutaView toView(RutaDeEntrega ruta) {
        List<RutaView.ParadaView> paradas = ruta.paradas().stream()
                .map(this::toParadaView)
                .toList();
        return new RutaView(
                ruta.id().toString(),
                ruta.repartidorId().valor(),
                ruta.fecha().valor(),
                ruta.estado().name(),
                paradas);
    }

    private RutaView.ParadaView toParadaView(Parada parada) {
        return new RutaView.ParadaView(
                parada.id().toString(),
                parada.paqueteId().valor(),
                parada.pacienteId().valor(),
                parada.orden(),
                parada.geolocalizacion().lat(),
                parada.geolocalizacion().lon(),
                parada.estado().name());
    }
}
