package org.bytefight.webserver.rabbitMQ.application;

import org.bytefight.webserver.gameMatch.domain.GameMatchResult;
import org.bytefight.webserver.gameMatch.application.GameMatchResultHandler;
import org.bytefight.webserver.rabbitMQ.infra.RabbitMQConfiguration;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "is-prod-env", havingValue = "true")
public class RabbitMQListener {

    private final GameMatchResultHandler gameMatchResultHandler;

    @Transactional
    @RabbitListener(queues = RabbitMQConfiguration.GAME_MATCH_RESULTS, errorHandler = "gameMatchListenerErrorHandler")
    public void receiveGameMatchResults(GameMatchResult gameMatchResult) {
        gameMatchResultHandler.handleGameMatchResult(gameMatchResult);
    }
}
