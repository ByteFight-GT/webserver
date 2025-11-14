package com.example.botfightwebserver.rabbitMQ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigurationTest {

    @InjectMocks
    private RabbitMQConfiguration configuration;

    private final String testUri = "amqp://guest:guest@localhost:5672";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(configuration, "rabbitMQUri", testUri);
    }

    @Test
    void connectionFactory() {
        ConnectionFactory factory = configuration.connectionFactory();

        assertTrue(factory instanceof CachingConnectionFactory);
    }

    @Test
    void gameMatchJobQueue() {
        Queue queue = configuration.gameMatchJobQueue();

        assertNotNull(queue);
        assertEquals(RabbitMQConfiguration.GAME_MATCH_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isAutoDelete());
        assertFalse(queue.isExclusive());
    }

    @Test
    void gameMatchResultQueue() {
        Queue queue = configuration.gameMatchResultQueue();

        assertNotNull(queue);
        assertEquals(RabbitMQConfiguration.GAME_MATCH_RESULTS, queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isAutoDelete());
        assertFalse(queue.isExclusive());
    }

    @Test
    void jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = configuration.jsonMessageConverter();
    }

    @Test
    void rabbitTemplate() {
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);

        RabbitTemplate template = configuration.rabbitTemplate(mockConnectionFactory);

        assertNotNull(template);
        assertEquals(mockConnectionFactory, template.getConnectionFactory());
        assertTrue(template.getMessageConverter() instanceof Jackson2JsonMessageConverter);
    }
}