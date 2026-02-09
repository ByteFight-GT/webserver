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
}
