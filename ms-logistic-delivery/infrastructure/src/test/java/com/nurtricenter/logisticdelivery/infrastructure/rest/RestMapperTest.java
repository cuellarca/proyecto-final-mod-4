package com.nurtricenter.logisticdelivery.infrastructure.rest;

import com.nurtricenter.logisticdelivery.application.usecase.command.ConfirmarEntregaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.application.usecase.command.RegistrarEntregaFallidaCommand;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.ConfirmarEntregaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.PlanificarRutaRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RegistrarFalloRequest;
import com.nurtricenter.logisticdelivery.infrastructure.rest.dto.RutaPlanificadaResponse;
import com.nurtricenter.logisticdelivery.infrastructure.rest.mapper.RestMapper;
import com.nurtricenter.logisticdelivery.infrastructure.rest.mapper.RestMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Traduccion del borde REST (DTO) al comando de aplicacion. Se usa la implementacion real que
 * genera MapStruct: los mappers no se mockean (testing-rules.md, LIMITES). Lo que se afirma es que
 * ningun dato se pierde ni se cruza al atravesar la frontera.
 */
class RestMapperTest {

    private final RestMapper mapper = new RestMapperImpl();

    @Test
    @DisplayName("HU-1: el pedido de planificacion viaja completo al comando, con sus paquetes")
    void traduceElPedidoDePlanificacionConSusPaquetes() {
        PlanificarRutaRequest request = new PlanificarRutaRequest(
                "rep-1", LocalDate.parse("2026-07-10"), -17.7833, -63.1821,
                List.of(new PlanificarRutaRequest.PaqueteRequest("pkg-1", "pac-1", -17.78, -63.18)));

        PlanificarRutaDelDiaCommand comando = mapper.toCommand(request);

        assertThat(comando).isEqualTo(new PlanificarRutaDelDiaCommand(
                "rep-1", LocalDate.parse("2026-07-10"), -17.7833, -63.1821,
                List.of(new PlanificarRutaDelDiaCommand.Paquete("pkg-1", "pac-1", -17.78, -63.18))));
    }

    @Test
    @DisplayName("HU-4: el id del path se combina con la evidencia del cuerpo")
    void combinaElIdDelPathConElCuerpoDeConfirmacion() {
        ConfirmarEntregaRequest request =
                new ConfirmarEntregaRequest(-17.78, -63.18, "https://storage/e.jpg", "Ana Lopez");

        ConfirmarEntregaCommand comando = mapper.toCommand("e-1", request);

        assertThat(comando).isEqualTo(new ConfirmarEntregaCommand(
                "e-1", -17.78, -63.18, "https://storage/e.jpg", "Ana Lopez"));
    }

    @Test
    @DisplayName("HU-5: el motivo del fallo llega al comando junto al id de la entrega")
    void combinaElIdDelPathConElMotivoDelFallo() {
        RegistrarFalloRequest request = new RegistrarFalloRequest(MotivoFallo.AUSENTE);

        RegistrarEntregaFallidaCommand comando = mapper.toCommand("e-1", request);

        assertThat(comando).isEqualTo(new RegistrarEntregaFallidaCommand("e-1", MotivoFallo.AUSENTE));
    }

    @Test
    @DisplayName("HU-1: el resultado del caso de uso se expone como respuesta REST")
    void traduceElResultadoAResponse() {
        PlanificarRutaResultado resultado = new PlanificarRutaResultado("r-1",
                List.of(new PlanificarRutaResultado.EntregaProgramada("e-1", "pkg-1", "pac-1")));

        RutaPlanificadaResponse response = mapper.toResponse(resultado);

        assertThat(response).isEqualTo(new RutaPlanificadaResponse("r-1",
                List.of(new RutaPlanificadaResponse.EntregaProgramadaResponse("e-1", "pkg-1", "pac-1"))));
    }
}
