package com.nurtricenter.logisticdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de arranque del microservicio {@code ms-logistic-delivery}.
 *
 * <p>Es el unico modulo ejecutable de la arquitectura hexagonal: compone las capas
 * (domain <- application <- infrastructure) y expone el contexto de Spring Boot.
 * El escaneo de componentes parte de {@code com.nurtricenter.logisticdelivery}, por
 * lo que alcanza los adaptadores de infraestructura y los casos de uso de aplicacion.</p>
 */
@SpringBootApplication
@EnableScheduling
public class LogisticDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticDeliveryApplication.class, args);
    }
}
