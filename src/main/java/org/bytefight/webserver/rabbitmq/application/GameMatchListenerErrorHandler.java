package org.bytefight.webserver.rabbitmq.application;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

@Component("gameMatchListenerErrorHandler")
@Slf4j
public class GameMatchListenerErrorHandler implements RabbitListenerErrorHandler {

  @Override
  public Object handleError(
      Message amqpMessage,
      Channel channel,
      org.springframework.messaging.Message<?> message,
      ListenerExecutionFailedException exception)
      throws Exception {
    log.error("Error in GameMatchListenerErrorHandler", exception);
    return null;
  }
}
