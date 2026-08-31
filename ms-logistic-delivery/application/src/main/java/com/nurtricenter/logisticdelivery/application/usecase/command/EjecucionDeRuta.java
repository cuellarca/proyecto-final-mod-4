package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de ejecucion de la ruta (HU-3): iniciar el recorrido y avanzar/completar/fallar sus
 * paradas. Toda operacion pasa por la raiz {@code RutaDeEntrega} (limite del agregado) y persiste
 * el cambio de estado en la misma transaccion.
 */
@Service
public class EjecucionDeRuta {

    private final RutaRepository rutaRepository;
    private final PublicadorDeEventos publicador;

    public EjecucionDeRuta(RutaRepository rutaRepository, PublicadorDeEventos publicador) {
        this.rutaRepository = rutaRepository;
        this.publicador = publicador;
    }

    @Transactional
    public void iniciar(String rutaId) {
        RutaDeEntrega ruta = cargar(rutaId);
        ruta.iniciar();
        persistir(ruta);
    }

    @Transactional
    public void avanzarParada(String rutaId, String paradaId) {
        RutaDeEntrega ruta = cargar(rutaId);
        ruta.avanzarParada(ParadaId.de(paradaId));
        persistir(ruta);
    }

    @Transactional
    public void completarParada(String rutaId, String paradaId) {
        RutaDeEntrega ruta = cargar(rutaId);
        ruta.completarParada(ParadaId.de(paradaId));
        persistir(ruta);
    }

    @Transactional
    public void fallarParada(String rutaId, String paradaId) {
        RutaDeEntrega ruta = cargar(rutaId);
        ruta.fallarParada(ParadaId.de(paradaId));
        persistir(ruta);
    }

    private RutaDeEntrega cargar(String rutaId) {
        return rutaRepository.buscarPorId(RutaId.de(rutaId))
                .orElseThrow(() -> new EntidadNoEncontradaException("Ruta no encontrada: " + rutaId));
    }

    private void persistir(RutaDeEntrega ruta) {
        rutaRepository.guardar(ruta);
        publicador.publicar(ruta.pullDomainEvents());
    }
}
