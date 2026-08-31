package com.nurtricenter.logisticdelivery.infrastructure.rest.mapper;

import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntregaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallidaCommand;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.ConfirmarEntregaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.PlanificarRutaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RegistrarFalloRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RutaPlanificadaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo REST DTO <-> comando de aplicacion con MapStruct (unico uso de MapStruct en el proyecto;
 * el mapeo JPA<->dominio va a mano). No toca el dominio: solo traduce los bordes.
 */
@Mapper(componentModel = "spring")
public interface RestMapper {

    PlanificarRutaDelDiaCommand toCommand(PlanificarRutaRequest request);

    PlanificarRutaDelDiaCommand.Paquete toCommand(PlanificarRutaRequest.PaqueteRequest paquete);

    RutaPlanificadaResponse toResponse(PlanificarRutaResultado resultado);

    RutaPlanificadaResponse.EntregaProgramadaResponse toResponse(PlanificarRutaResultado.EntregaProgramada entrega);

    @Mapping(target = "entregaId", source = "entregaId")
    ConfirmarEntregaCommand toCommand(String entregaId, ConfirmarEntregaRequest request);

    @Mapping(target = "entregaId", source = "entregaId")
    RegistrarEntregaFallidaCommand toCommand(String entregaId, RegistrarFalloRequest request);
}
