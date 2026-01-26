package org.bytefight.webserver.rabbitMQ.application;

import org.bytefight.webserver.gameMatch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gameMatch.domain.GameMatchResult;
import org.bytefight.webserver.rabbitMQ.infra.RabbitMQConfiguration;
import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;

    public void enqueueGameMatchJob(GameMatchJob job) {
        int priority;
//        if (job.reason() == MatchReason.VALIDATION) {
//            priority = 6;
//        } else if (job.reason() == MatchReason.TOURNAMENT){
//            priority = 5;
//        } else if (job.reason() == MatchReason.LADDER) {
//            priority = 4;
//        } else {
//            priority = 3;
//        }
        rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_QUEUE, job, message -> {
//            message.getMessageProperties().setPriority(priority);
            return message;
        });
    }

    public List<GameMatchJob> peekGameMatchQueue() {
        List<GameMatchJob> messages = new ArrayList<>();
        boolean hasMore = true;

        while (hasMore) {
            GameMatchJob message = (GameMatchJob) rabbitTemplate.receiveAndConvert(
                RabbitMQConfiguration.GAME_MATCH_QUEUE,
                1000
            );
            if (message == null) {
                hasMore = false;
            } else {
                messages.add(message);
            }
        }
        for (GameMatchJob message : messages) {
            rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_QUEUE, message);
        }

        return messages;
    }

    public List<GameMatchJob> deleteGameMatchQueue() {
        List<GameMatchJob> messages = new ArrayList<>();
        boolean hasMore = true;
        while (hasMore) {
            GameMatchJob message = (GameMatchJob) rabbitTemplate.receiveAndConvert(
                RabbitMQConfiguration.GAME_MATCH_QUEUE,
                1000
            );
            if (message == null) {
                hasMore = false;
            } else {
               messages.add(message);
            }
        }
        return messages;
    }

    public List<GameMatchResult> deleteGameResultQueue() {
        List<GameMatchResult> messages = new ArrayList<>();
        boolean hasMore = true;
        while (hasMore) {
            GameMatchResult message = (GameMatchResult) rabbitTemplate.receiveAndConvert(
                RabbitMQConfiguration.GAME_MATCH_RESULTS,
                1000
            );
            if (message == null) {
                hasMore = false;
            } else {
                messages.add(message);
            }
        }
        return messages;
    }


    //only to be used for testing
    @VisibleForTesting
    public void enqueueGameMatchResult(GameMatchResult result) {
        rabbitTemplate.convertAndSend(RabbitMQConfiguration.GAME_MATCH_RESULTS, result);
    }
}
