package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import com.example.botfightwebserver.player.domain.SelfPlayerDto;
import com.example.botfightwebserver.player.domain.UpdatePlayerProfileDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.TeamDeletionReason;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Player (Private)")
@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Validated
public class PrivatePlayerController {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final PermissionsService permissionsService;

    @Operation(
            operationId = "getCurrentPlayer",
            summary = "Get current player profile"
    )
    @GetMapping("/me")
    public ResponseEntity<SelfPlayerDto> getCurrentPlayer(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                SelfPlayerDto.from(playerService.getPlayer(user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
        );
    }

    @Operation(
            operationId = "updateCurrentPlayer",
            summary = "Update current player profile"
    )
    @PatchMapping("/me")
    public ResponseEntity<SelfPlayerDto> updateCurrentPlayer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePlayerProfileDto input
    ) {
//        permissionsService.validateAllowUpdateProfile();
        Player player = playerService.getPlayer(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(input.getUsername() != null) {
            playerService.setUsername(player, input.getUsername());
        }

        return ResponseEntity.ok(SelfPlayerDto.from(player));
    }
}
