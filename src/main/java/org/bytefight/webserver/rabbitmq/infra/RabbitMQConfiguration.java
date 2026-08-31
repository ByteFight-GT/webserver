package org.bytefight.webserver.rabbitmq.infra;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

  @Value("${spring.rabbitmq.uri}")
  private String rabbitMQUri;

  public static final String GAME_MATCH_EXCHANGE = "match.schedule";
  public static final String GAME_MATCH_UPDATES = "match.updates";
  public static final String GAME_MATCH_RESULTS = "match.results";

  public static final String RETRY_EXCHANGE = "match.retry"; // a dlx where failed matches go
  public static final String RETRY_QUEUE = "match.retry.30s";

  @Bean
  public ConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
    connectionFactory.setUri(rabbitMQUri);
    return connectionFactory;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
  }

  @Bean
  public Queue gameMatchUpdateQueue() {
    return new Queue(GAME_MATCH_UPDATES, true);
  }

  @Bean
  public Queue gameMatchResultQueue() {
    return new Queue(GAME_MATCH_RESULTS, true);
  }

  @Bean
  public TopicExchange gameMatchExchange() {
    return new TopicExchange(GAME_MATCH_EXCHANGE, true, false);
  }

  @Bean
  public FanoutExchange matchRetryExchange() {
    return ExchangeBuilder.fanoutExchange(RETRY_EXCHANGE).durable(true).build();
  }

  @Bean
  public Queue matchRetryQueue() {
    /*
    Matches in this queue rest for 30 seconds and then get sent to the game match exchange
     */
    return QueueBuilder.durable(RETRY_QUEUE)
        .ttl(30_000)
        .deadLetterExchange(GAME_MATCH_EXCHANGE)
        .build();
  }

  @Bean
  public Binding matchRetryBinding() {
    return BindingBuilder.bind(matchRetryQueue()).to(matchRetryExchange());
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }
}
