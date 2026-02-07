package org.bytefight.webserver.rabbitmq.application;

import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.rabbitmq.infra.RabbitMQConfiguration;
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
        rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GAME_MATCH_EXCHANGE,
                "competition." + job.getCompetitionSlug() + "." + job.getLadder(),
                job
        );
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
