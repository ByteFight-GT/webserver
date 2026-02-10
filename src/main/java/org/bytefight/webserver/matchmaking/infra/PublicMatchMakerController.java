package org.bytefight.webserver.matchmaking.infra;

import org.bytefight.webserver.matchmaking.application.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/matches")
@RequiredArgsConstructor
public class PublicMatchMakerController {
    private final MatchMakingProperties props;
    private final MatchmakingService matchMakingService;
}