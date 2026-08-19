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

  /**
   * Maximum number of times a match may be (re)scheduled before the stale sweep gives up and marks
   * it failed instead of requeuing it forever. A match is scheduled once on creation, so this is the
   * total attempt count, not the retry count.
   */
  private int maxReschedules = 5;
}
