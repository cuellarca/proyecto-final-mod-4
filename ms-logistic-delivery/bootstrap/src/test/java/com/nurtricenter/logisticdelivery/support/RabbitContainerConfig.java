package com.nurtricenter.logisticdelivery.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Arranca un contenedor RabbitMQ para los tests de integracion y lo cablea a Spring AMQP via
 * {@link ServiceConnection}. Se combina con {@link PostgisContainerConfig} en los tests de la saga.
 * Un broker por contexto: si dos contextos comparten uno, sus listeners compiten por los mismos
 * mensajes y el evento le llega al contexto equivocado.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitContainerConfig {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    public RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
    }
}
