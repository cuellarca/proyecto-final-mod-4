package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.message.PaquetesListosParaEntregaMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Borde AMQP de entrada (HU-1): el mensaje que publica el BC de Produccion se traduce al comando de
 * planificacion. El caso de uso es el seam; lo que se afirma es la traduccion del contrato externo,
 * que es justo donde se rompen las integraciones.
 */
@ExtendWith(MockitoExtension.class)
class PaquetesListosListenerTest {

    @Mock
    PlanificarRutaDelDia planificarRutaDelDia;

    @Test
    @DisplayName("HU-1: el mensaje de Produccion se traduce al comando con todos sus paquetes")
    void traduceElMensajeAlComandoDePlanificacion() {
        when(planificarRutaDelDia.ejecutar(any()))
                .thenReturn(new PlanificarRutaResultado("r-1", List.of()));
        PaquetesListosParaEntregaMessage mensaje = new PaquetesListosParaEntregaMessage(
                "rep-1", LocalDate.parse("2026-07-10"), -17.7833, -63.1821,
                List.of(new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-1", "pac-1", -17.78, -63.18),
                        new PaquetesListosParaEntregaMessage.PaqueteMsg("pkg-2", "pac-2", -17.79, -63.19)));

        new PaquetesListosListener(planificarRutaDelDia).onPaquetesListos(mensaje);

        verify(planificarRutaDelDia).ejecutar(new PlanificarRutaDelDiaCommand(
                "rep-1", LocalDate.parse("2026-07-10"), -17.7833, -63.1821,
                List.of(new PlanificarRutaDelDiaCommand.Paquete("pkg-1", "pac-1", -17.78, -63.18),
                        new PlanificarRutaDelDiaCommand.Paquete("pkg-2", "pac-2", -17.79, -63.19))));
    }
}
