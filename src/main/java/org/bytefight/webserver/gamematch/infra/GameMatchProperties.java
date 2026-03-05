package org.bytefight.webserver.gamematch.infra;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gamematch")
public class GameMatchProperties {
  private long staleThresholdMinutes;
  private boolean requeueStale;
}
