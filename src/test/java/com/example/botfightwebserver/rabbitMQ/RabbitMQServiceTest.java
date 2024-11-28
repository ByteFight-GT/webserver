package com.example.botfightwebserver.rabbitMQ;

import com.example.botfightwebserver.gameMatch.GameMatchJob;
import com.example.botfightwebserver.gameMatchResult.GameMatchResult;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.submission.STORAGE_SOURCE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQService rabbitMQService;

    private static final Random random = new Random();

    @BeforeEach
    void setUp() {
        rabbitMQService = new RabbitMQService(rabbitTemplate);
    }

    @Test
    void enqueueGameMatchJob_shouldSendMessageToQueue() {
        GameMatchJob gameMatchJob =
            new GameMatchJob(1L, "fake path", "fake path 2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, "default map");

        rabbitMQService.enqueueGameMatchJob(gameMatchJob);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            eq(gameMatchJob)
        );
    }

    @Test
    void peekGameMatchQueue_shouldReturnAllMessagesAndRequeueThem() {
        GameMatchJob gameMatchJob1 =
            new GameMatchJob(1L, "fake path", "fake path 2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, "default map");

        GameMatchJob gameMatchJob2 =
            new GameMatchJob(2L, "fake path", "fake path 2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, "default map");

        when(rabbitTemplate.receiveAndConvert(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            anyLong()
        )).thenReturn(gameMatchJob1, gameMatchJob2, null);

        List<GameMatchJob> result = rabbitMQService.peekGameMatchQueue();

        assertEquals(2, result.size());
        assertTrue(result.contains(gameMatchJob1));
        assertTrue(result.contains(gameMatchJob2));

        verify(rabbitTemplate, times(1)).convertAndSend(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            eq(gameMatchJob1)
        );

        verify(rabbitTemplate, times(1)).convertAndSend(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            eq(gameMatchJob2)
        );
    }

    @Test
    void deleteGameMatchQueue_shouldReturnAllMessagesWithoutRequeueing() {
        GameMatchJob gameMatchJob1 =
            new GameMatchJob(1L, "fake path", "fake path 2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, "default map");

        GameMatchJob gameMatchJob2 =
            new GameMatchJob(2L, "fake path", "fake path 2", STORAGE_SOURCE.GCP, STORAGE_SOURCE.GCP, "default map");


        when(rabbitTemplate.receiveAndConvert(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            anyLong()
        )).thenReturn(gameMatchJob1, gameMatchJob2, null);

        List<GameMatchJob> result = rabbitMQService.deleteGameMatchQueue();

        assertEquals(2, result.size());
        assertTrue(result.contains(gameMatchJob1));
        assertTrue(result.contains(gameMatchJob2));

        verify(rabbitTemplate, never()).convertAndSend(
            eq(RabbitMQConfiguration.GAME_MATCH_QUEUE),
            any(GameMatchJob.class)
        );
    }

    @Test
    void enqueueGameMatchResult_shouldSendResultToQueue() {
        GameMatchResult result = new GameMatchResult(1L, MATCH_STATUS.TEAM_ONE_WIN, "some logs");

        rabbitMQService.enqueueGameMatchResult(result);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfiguration.GAME_MATCH_RESULTS),
            eq(result)
        );
    }
}
