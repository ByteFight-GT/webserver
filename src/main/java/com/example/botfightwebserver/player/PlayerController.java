package com.example.botfightwebserver.player;

import com.example.botfightwebserver.team.TeamDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/players")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        List<PlayerDTO> players = playerService.getPlayers().stream().map(PlayerDTO::fromEntity).toList();
        return ResponseEntity.ok(players);
    }

    @GetMapping("/player")
    public ResponseEntity<PlayerDTO> getPlayerById(@RequestParam Long id) {
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.getPlayer(id)));
    }

    @PostMapping("/create")
    public ResponseEntity<PlayerDTO> createPlayer(@RequestParam String name, @RequestParam String email,
                                                  @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.createPlayer(name, email, teamId)));
    }

    @PostMapping("/team")
    public ResponseEntity<PlayerDTO> assignTeam(@RequestParam Long playerId, @RequestParam Long teamId) {
        return ResponseEntity.ok(PlayerDTO.fromEntity(playerService.setPlayerTeam(playerId, teamId)));
    }

}
