package com.nurtricenter.logisticdelivery.contrato;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDia;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaDelDiaCommand;
import com.nurtricenter.logisticdelivery.application.usecase.command.PlanificarRutaResultado;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verificacion del contrato de {@code app-repartidor} (lado provider).
 *
 * <p>Pact reproduce cada interaccion de {@code pacts/app-repartidor-ms-logistic-delivery.json} contra
 * la aplicacion completa: borde REST, casos de uso, JPA y PostGIS reales, con los mismos contenedores
 * que el resto de las pruebas de integracion ({@link PruebaDeIntegracion}). Antes de cada interaccion
 * corre el {@code @State} que la prepara; nada dentro del proceso esta doblado.
 *
 * <p>Reglas: {@code contract-testing-rules.md}.
 */
@PruebaDeIntegracion
@Provider("ms-logistic-delivery")
@PactFolder("../pacts")
class ContratoAppRepartidorPactIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PlanificarRutaDelDia planificarRutaDelDia;

    @BeforeEach
    void apuntarAlServicio(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    @DisplayName("HU-1/HU-4 · flujo correcto: el servicio cumple cada interaccion del contrato de app-repartidor")
    void cumpleElContratoDeAppRepartidor(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /** El consumidor manda el repartidor y la fecha; aqui se siembra una ruta exactamente para ellos. */
    @State("el repartidor tiene una ruta planificada para la fecha")
    void rutaPlanificada(Map<String, Object> parametros) {
        planificar((String) parametros.get("repartidorId"), LocalDate.parse((String) parametros.get("fecha")));
    }

    /**
     * El id de la entrega lo genera el servicio: se devuelve para que Pact lo inyecte en el path de la
     * solicitud ({@code pathFromProviderState} del lado consumer). Repartidor unico: no depende de lo
     * que haya sembrado otra prueba.
     */
    @State("hay una entrega pendiente de confirmar")
    Map<String, Object> entregaPendiente() {
        PlanificarRutaResultado ruta = planificar("rep-pact-" + UUID.randomUUID(), LocalDate.of(2026, 10, 6));
        return Map.of("entregaId", ruta.entregas().get(0).entregaId());
    }

    private PlanificarRutaResultado planificar(String repartidorId, LocalDate fecha) {
        String sufijo = UUID.randomUUID().toString();
        return planificarRutaDelDia.ejecutar(new PlanificarRutaDelDiaCommand(
                repartidorId, fecha, -17.78, -63.18, List.of(
                        new PlanificarRutaDelDiaCommand.Paquete("pkg-pact-" + sufijo, "pac-pact-" + sufijo, -17.79, -63.19))));
    }
}
