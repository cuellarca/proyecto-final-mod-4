package com.nurtricenter.logisticdelivery.support;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cableado comun de las pruebas de integracion: la aplicacion completa contra PostGIS y RabbitMQ
 * levantados por Testcontainers.
 *
 * <p>Spring cachea un contexto por combinacion de configuracion. Al compartir esta anotacion, todas
 * las clases que la usan comparten un unico contexto y, por lo tanto, un unico par de contenedores.
 * Una clase que necesite propiedades propias declara sus anotaciones por separado y paga su propio
 * contexto: es el caso de {@code DegradacionMapasIT}, que necesita el proveedor de mapas caido.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        classes = LogisticDeliveryApplication.class,
        properties = {
                "logistic.reintentos.maximo=1",
                "logistic.outbox.poll-delay-ms=500"
        })
@AutoConfigureMockMvc
@Import({PostgisContainerConfig.class, RabbitContainerConfig.class})
public @interface PruebaDeIntegracion {
}
