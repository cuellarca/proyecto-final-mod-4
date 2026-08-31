package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer AMQP del evento de entrada {@code PaquetesListosParaEntrega} (HU-1): dispara el caso de
 * uso {@code PlanificarRutaDelDia} con los paquetes ya geolocalizados que llegan del BC de Produccion.
 */
@Component
public class PaquetesListosListener {

    private static final Logger log = LoggerFactory.getLogger(PaquetesListosListener.class);

    private final PlanificarRutaDelDia planificarRutaDelDia;

    public PaquetesListosListener(PlanificarRutaDelDia planificarRutaDelDia) {
        this.planificarRutaDelDia = planificarRutaDelDia;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_PAQUETES_LISTOS)
    public void onPaquetesListos(PaquetesListosParaEntregaMessage mensaje) {
        log.info("[amqp] PaquetesListosParaEntrega recibido: repartidor={}, fecha={}, {} paquetes",
                mensaje.repartidorId(), mensaje.fecha(), mensaje.paquetes().size());

        PlanificarRutaDelDiaCommand comando = new PlanificarRutaDelDiaCommand(
                mensaje.repartidorId(),
                mensaje.fecha(),
                mensaje.origenLat(),
                mensaje.origenLon(),
                mensaje.paquetes().stream()
                        .map(p -> new PlanificarRutaDelDiaCommand.Paquete(p.paqueteId(), p.pacienteId(), p.lat(), p.lon()))
                        .toList());

        PlanificarRutaResultado resultado = planificarRutaDelDia.ejecutar(comando);
        log.info("[amqp] Ruta {} planificada con {} entregas", resultado.rutaId(), resultado.entregas().size());
    }
}
