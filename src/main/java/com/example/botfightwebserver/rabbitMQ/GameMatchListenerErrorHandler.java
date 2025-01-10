package com.example.botfightwebserver.rabbitMQ;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component("gameMatchListenerErrorHandler")
@Slf4j
public class GameMatchListenerErrorHandler implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(Message amqpMessage, Channel channel, org.springframework.messaging.Message<?> message,
                              ListenerExecutionFailedException exception) throws Exception {
        log.error("Error in GameMatchListenerErrorHandler", exception);
        return null;
    }

    @Override
    public Object handleError(Message message, org.springframework.messaging.Message<?> message1,
                              ListenerExecutionFailedException e) throws Exception {
        return null;
    }
}