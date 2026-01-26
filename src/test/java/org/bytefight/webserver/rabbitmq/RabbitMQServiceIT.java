package org.bytefight.webserver.rabbitmq;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.rabbitmq.application.RabbitMQService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQServiceIT extends FullStackIntegrationTestBase {
    @Autowired
    private RabbitMQService rabbitMQService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange gameMatchExchange;

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @Test
    void enqueueGameMatchJobRoutesByCompetitionAndLadder() {
        String queueName = "test.match.queue";
        Queue queue = QueueBuilder.nonDurable(queueName).autoDelete().build();
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(gameMatchExchange);
        admin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue)
                .to(gameMatchExchange)
                .with("competition.test.ladder1");
        admin.declareBinding(binding);

        GameMatchJob job = new GameMatchJob(
                "match-1",
                "test",
                "team-a",
                "team-b",
                "sub-a",
                "sub-b",
                "ladder1",
                MatchReason.ladder,
                Map.of("map", "arena_01")
        );

        rabbitMQService.enqueueGameMatchJob(job);

        Object message = rabbitTemplate.receiveAndConvert(queueName, 2000);
        assertThat(message).isInstanceOf(GameMatchJob.class);

        GameMatchJob received = (GameMatchJob) message;
        assertThat(received.getCompetitionSlug()).isEqualTo("test");
        assertThat(received.getLadder()).isEqualTo("ladder1");
        assertThat(received.getUuid()).isEqualTo("match-1");
    }
}
