package com.example.botfightwebserver.leaderboard;

import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/leaderboard")
public class LeaderboardController {

    private final TeamService teamService;

    @GetMapping("/all")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard() {
        return ResponseEntity.ok(teamService.getLeaderboard());
    }

    @GetMapping("/all/paginated")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(teamService.getLeaderboard(page,size));
    }
}

