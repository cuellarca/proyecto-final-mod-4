package com.nurtricenter.logisticdelivery.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Arranca un contenedor Postgres+PostGIS para los tests de integracion y lo cablea al
 * datasource de Spring via {@link ServiceConnection}. La imagen {@code postgis/postgis}
 * se declara compatible con {@code postgres} para reutilizar {@link PostgreSQLContainer}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgisContainerConfig {

    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGIS_IMAGE)
                .withDatabaseName("logistic")
                .withUsername("logistic")
                .withPassword("logistic");
    }
}
