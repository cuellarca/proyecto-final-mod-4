package com.nurtricenter.logisticdelivery.infrastructure.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajusta la serializacion JSON del <b>published language</b>: los Value Object de identidad se
 * emiten como texto plano ({@code "entregaId": "uuid"}) en vez de objetos anidados
 * ({@code {"valor": "uuid"}}). Solo afecta la serializacion de eventos; el dominio no se toca.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule idsComoTextoModule() {
        SimpleModule module = new SimpleModule("ids-como-texto");
        module.addSerializer(EntregaId.class, new ToStringSerializer());
        module.addSerializer(RutaId.class, new ToStringSerializer());
        module.addSerializer(ParadaId.class, new ToStringSerializer());
        module.addSerializer(PacienteId.class, new ToStringSerializer());
        module.addSerializer(PaqueteId.class, new ToStringSerializer());
        module.addSerializer(RepartidorId.class, new ToStringSerializer());
        module.addSerializer(Url.class, new ToStringSerializer());
        return module;
    }
}
