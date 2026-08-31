package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.application.port.out.HistorialReadModel;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Query CQRS (HU-6): constancia (evidencia) de una entrega, leida del read model.
 */
@Service
public class ConsultarConstanciaDeEntrega {

    private final HistorialReadModel readModel;

    public ConsultarConstanciaDeEntrega(HistorialReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<ConstanciaView> ejecutar(String entregaId) {
        return readModel.constanciaDe(entregaId);
    }
}
