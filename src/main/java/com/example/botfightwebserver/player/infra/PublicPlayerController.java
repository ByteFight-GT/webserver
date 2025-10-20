package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.PublicPlayerDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "Player (Public)")
@RestController
@RequestMapping("/api/v1/public/player")
@RequiredArgsConstructor
@Validated
public class PublicPlayerController {
    private final PlayerService playerService;

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = !playerService.isUsernameExist(username);
        return ResponseEntity.ok(Collections.singletonMap("available", isAvailable));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getPlayerCount() {
        return ResponseEntity.ok(playerService.getNumberPlayers());
    }

    @GetMapping("/team-id")
    public ResponseEntity<List<PublicPlayerDto>> getPlayersByTeamId(@RequestParam Long teamId) {
        return ResponseEntity.ok(playerService.getPlayersByTeam(teamId).stream().map(PublicPlayerDto::from).toList());
    }
}
