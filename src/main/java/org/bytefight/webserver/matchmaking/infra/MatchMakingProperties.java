package org.bytefight.webserver.matchmaking.infra;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "matchmaking")
public class MatchMakingProperties {
  private boolean enabled = false;
  private String cron;
  private String tz;

  /**
   * Maximum matches a single matchmaking event may enqueue. Null or non-positive means unlimited
   * (the historical behaviour). Set this to bound a single cron tick to something the runner fleet
   * can clear before the next tick, otherwise the backlog is monotonic. The server can't derive
   * fleet size itself, so this is an ops-tuned value.
   */
  private Integer maxMatchesPerEvent;
}
