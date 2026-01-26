package org.bytefight.webserver.matchMaking.infra;

import org.bytefight.webserver.matchMaking.application.MatchMakingService;
import org.bytefight.webserver.matchMaking.application.ScheduledMatchMaker;
import org.bytefight.webserver.matchMaking.domain.MatchMakingStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/v1/public/matches")
@RequiredArgsConstructor
public class PublicMatchMakerController {
    private final ScheduledMatchMaker scheduler;
    private final MatchMakingProperties props;
    private final MatchMakingService matchMakingService;

    @GetMapping("/status")
    public ResponseEntity<MatchMakingStatusDto> getMatchMakingStatus() {
        ZoneId zone = ZoneId.of(props.getTz() == null ? "UTC" : props.getTz());
        CronExpression expr = CronExpression.parse(props.getCron());
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = expr.next(now);

        return ResponseEntity.ok(
                MatchMakingStatusDto.builder()
                        .running(matchMakingService.isEnabled())
                        .lastRunAt(scheduler.getLastRun())
                        .nextRunAt(matchMakingService.isEnabled() ? next.toInstant() : null)
                        .build()
        );
    }
}