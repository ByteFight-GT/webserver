package org.bytefight.webserver.gamematchfile.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematchfile.application.GameMatchFileService;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.gamematchfile.domain.dto.GameMatchFileDto;
import org.bytefight.webserver.gamematchfile.domain.dto.GameMatchFileUploadDto;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(
    name = "Game Match File",
    description = "Endpoints for uploading and retrieving game match result files")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-match-file")
public class GameMatchFileController {
  private final TeamService teamService;
  private final GameMatchService gameMatchService;
  private final GameMatchFileService gameMatchFileService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GameMatchFileDto> uploadGameMatchFile(
      @AuthenticationPrincipal User user, GameMatchFileUploadDto uploadDto) {
    GameMatch gameMatch =
        gameMatchService
            .getGameMatch(UUID.fromString(uploadDto.getGameMatchUuid()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Team team =
        uploadDto.getTeamUuid() != null
            ? teamService
                .getTeamByUuid(UUID.fromString(uploadDto.getTeamUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
            : null;

    GameMatchFile gameMatchFile;

    try {
      gameMatchFile =
          gameMatchFileService.uploadGameMatchFile(
              uploadDto.getFile(), uploadDto.getSlug(), gameMatch, uploadDto.getVisibility(), team);
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();

      //            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      //                    .body(e.getMessage() + " - " +
      // java.util.Arrays.toString(e.getStackTrace()));
    }

    return ResponseEntity.ok(GameMatchFileDto.from(gameMatchFile, null));
  }

  @GetMapping("/{matchUuid}/{slug}")
  @Operation(
      operationId = "getCommonGameMatchFile",
      summary = "Get a game match file that is unassociated with any team")
  public ResponseEntity<GameMatchFileDto> getCommonGameMatchFile(
      @AuthenticationPrincipal User user, @PathVariable UUID matchUuid, @PathVariable String slug) {
    GameMatchFile gameMatchFile =
        gameMatchFileService
            .getByGameMatchAndSlugNoTeam(matchUuid, slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    DownloadLinkDto downloadLinkDto = gameMatchFileService.getDownloadLink(user, gameMatchFile);
    return ResponseEntity.ok(GameMatchFileDto.from(gameMatchFile, downloadLinkDto));
  }

  @GetMapping("/{matchUuid}/{slug}/team/{teamUuid}")
  @Operation(operationId = "getTeamGameMatchFile", summary = "Get a team-specific game match file")
  public ResponseEntity<GameMatchFileDto> getTeamGameMatchFile(
      @AuthenticationPrincipal User user,
      @PathVariable UUID matchUuid,
      @PathVariable String slug,
      @PathVariable UUID teamUuid) {
    Team team =
        teamService
            .getTeamByUuid(teamUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    GameMatchFile gameMatchFile =
        gameMatchFileService
            .getByGameMatchAndSlugAndTeam(matchUuid, slug, team)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    DownloadLinkDto downloadLinkDto = gameMatchFileService.getDownloadLink(user, gameMatchFile);
    return ResponseEntity.ok(GameMatchFileDto.from(gameMatchFile, downloadLinkDto));
  }
}
