package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ. Un unico exchange {@code topic} para todos los eventos del ecosistema; las
 * routing keys distinguen el tipo. Aqui se declara el lado de <b>entrada</b> (HU-1): la cola donde
 * Logistica escucha {@code PaquetesListosParaEntrega}. El lado de salida (eventos de dominio) y las
 * colas de los consumidores stub se declaran en la Fase 4.
 */
@Configuration
public class RabbitTopologyConfig {

    /** Exchange topico comun del ecosistema Nur-tricenter. */
    public static final String EXCHANGE = "nurtricenter.eventos";

    /** Routing key del evento de entrada que emite Produccion. */
    public static final String RK_PAQUETES_LISTOS = "produccion.paquetes-listos-para-entrega";

    /** Cola donde Logistica consume los paquetes listos. */
    public static final String QUEUE_PAQUETES_LISTOS = "logistica.paquetes-listos-para-entrega";

    /** Cola del stub de ms-notificaciones (avisos al paciente): entregas confirmadas y fallidas. */
    public static final String QUEUE_NOTIFICACIONES = "ms-notificaciones.avisos";

    /** Cola del stub de ms-catering-suscripcion: entregas no concretadas (reprogramacion a dia futuro). */
    public static final String QUEUE_CATERING = "ms-catering.reprogramaciones";

    /** Cola del proyector del read model (HU-6): todos los eventos de la entrega. */
    public static final String QUEUE_PROYECCION = "logistica.proyeccion-historial";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // --- Entrada (HU-1): Logistica consume PaquetesListosParaEntrega ---

    @Bean
    public Queue paquetesListosQueue() {
        return QueueBuilder.durable(QUEUE_PAQUETES_LISTOS).build();
    }

    @Bean
    public Binding paquetesListosBinding(Queue paquetesListosQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(paquetesListosQueue).to(eventosExchange).with(RK_PAQUETES_LISTOS);
    }

    // --- Salida: los eventos de dominio de Logistica hacia los consumidores stub (Fase 4) ---

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICACIONES).build();
    }

    @Bean
    public Binding notificacionesConfirmadaBinding(Queue notificacionesQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_CONFIRMADA);
    }

    @Bean
    public Binding notificacionesFallidaBinding(Queue notificacionesQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_FALLIDA);
    }

    @Bean
    public Queue cateringQueue() {
        return QueueBuilder.durable(QUEUE_CATERING).build();
    }

    @Bean
    public Binding cateringNoConcretadaBinding(Queue cateringQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(cateringQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_NO_CONCRETADA);
    }

    // --- Proyeccion del read model (HU-6): la cola recibe todos los eventos de la entrega ---

    @Bean
    public Queue proyeccionQueue() {
        return QueueBuilder.durable(QUEUE_PROYECCION).build();
    }

    @Bean
    public Binding proyeccionConfirmadaBinding(Queue proyeccionQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(proyeccionQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_CONFIRMADA);
    }

    @Bean
    public Binding proyeccionFallidaBinding(Queue proyeccionQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(proyeccionQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_FALLIDA);
    }

    @Bean
    public Binding proyeccionReprogramadaBinding(Queue proyeccionQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(proyeccionQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_REPROGRAMADA);
    }

    @Bean
    public Binding proyeccionNoConcretadaBinding(Queue proyeccionQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(proyeccionQueue).to(eventosExchange).with(RoutingKeys.ENTREGA_NO_CONCRETADA);
    }

    /**
     * Conversor JSON (Jackson) para AMQP. Usa el tipo <b>inferido</b> del parametro del listener,
     * de modo que no depende del header {@code __TypeId__} del productor.
     */
    @Bean
    public Jackson2JsonMessageConverter jacksonRabbitConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("com.nurtricenter.logisticdelivery.*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
