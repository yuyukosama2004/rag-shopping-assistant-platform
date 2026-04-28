package com.biyesheji.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.submit.queue";
    public static final String ORDER_ROUTING_KEY = "order.submit";

    @Bean
    public Queue orderSubmitQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey("order.cancel")
                .build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable("order.cancel.queue").build();
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding submitBinding() {
        return BindingBuilder.bind(orderSubmitQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding cancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderExchange())
                .with("order.cancel");
    }
}
