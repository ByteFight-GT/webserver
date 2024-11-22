package com.example.botfightwebserver.rabbitMQ;

import com.example.botfightwebserver.gameMatchResult.GameMatchResult;
import com.example.botfightwebserver.gameMatchResult.GameMatchResultHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {

    private final GameMatchResultHandler gameMatchResultHandler;

    @Transactional
    @RabbitListener(queues = RabbitMQConfiguration.GAME_MATCH_RESULTS)
    public void receiveGameMatchResults(GameMatchResult gameMatchResult) {
        gameMatchResultHandler.handleGameMatchResult(gameMatchResult);
    }
}
