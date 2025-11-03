package com.example.botfightwebserver.gameMatch.infra;

import com.example.botfightwebserver.gameMatch.application.GameMatchSearchService;
import com.example.botfightwebserver.gameMatch.application.GameMatchService;
import com.example.botfightwebserver.gameMatch.domain.GameMatchDto;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match")
public class PublicGameMatchController {
    private final GameMatchSearchService searchService;
    private final GameMatchService gameMatchService;

    @GetMapping("/search")
    public ResponseEntity<Page<GameMatchDto>> searchGameMatches(
            @RequestParam(required = false) String teamSearchParam,
            @RequestParam(required = false) String teamUuid,
            @RequestParam(required = false) MATCH_REASON reason,
            @RequestParam(required = false) String map,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GameMatchDto> gameSearchResult = searchService.searchGame(Optional.ofNullable(teamSearchParam), Optional.ofNullable(teamUuid),
                Optional.ofNullable(reason), pageable);
        return ResponseEntity.ok(gameSearchResult);
    }

    @GetMapping("/logs/paginated")
    public ResponseEntity<Page<GameMatchDto>> paginateGameMatches(
            @RequestParam String teamUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String otherTeamId
    ) {
        if (otherTeamId != null) {
            return ResponseEntity.ok(gameMatchService.getTeamMatches(teamUuid, otherTeamId, page, size));
        }
        return ResponseEntity.ok(gameMatchService.getTeamMatches(teamUuid, page, size));
    }
}
