package org.bytefight.webserver.gamematchfile.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.gamematchfile.application.GameMatchFileService;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.bytefight.webserver.gamematchfile.domain.dto.GameMatchFileDto;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Game Match File (Public)", description = "Endpoints for uploading and retrieving game match result files")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/game-match-file")
public class PublicGameMatchFileController {
    private final GameMatchFileService gameMatchFileService;

    @GetMapping("/{matchUuid}/{slug}")
    @Operation(
            operationId = "getPublicGameMatchFile",
            summary = "Get a public game match file"
    )
    public ResponseEntity<GameMatchFileDto> getPublicGameMatchFile(
            @PathVariable UUID matchUuid,
            @PathVariable String slug
    ) {
        GameMatchFile gameMatchFile = gameMatchFileService.getByGameMatchAndSlugNoTeam(matchUuid, slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DownloadLinkDto downloadLinkDto = gameMatchFileService.getPublicDownloadLink(gameMatchFile);
        return ResponseEntity.ok(GameMatchFileDto.from(gameMatchFile, downloadLinkDto));
    }
}
