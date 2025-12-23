package com.example.botfightwebserver.rabbitMQ;

import com.example.botfightwebserver.gameMatch.domain.GameMatchResult;
import com.example.botfightwebserver.gameMatch.application.GameMatchResultHandler;
import com.example.botfightwebserver.gameMatch.domain.MatchStatus;
import com.example.botfightwebserver.rabbitMQ.application.RabbitMQListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQListenerTest {

    @Mock
    private GameMatchResultHandler gameMatchResultHandler;

    private RabbitMQListener rabbitMQListener;

    @BeforeEach
    void setUp() {
        rabbitMQListener = new RabbitMQListener(gameMatchResultHandler);
    }

    @Test
    void receiveGameMatchResults_ShouldProcessMessageSuccessfully() {
        GameMatchResult gameMatchResult = new GameMatchResult(1L, MatchStatus.TEAM_TWO_WIN, "SOME LOGS");

        rabbitMQListener.receiveGameMatchResults(gameMatchResult);

        verify(gameMatchResultHandler, times(1)).handleGameMatchResult(gameMatchResult);
    }
}