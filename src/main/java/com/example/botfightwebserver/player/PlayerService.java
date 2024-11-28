package com.example.botfightwebserver.player;

import com.example.botfightwebserver.team.TeamRepository;
import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamService teamService;

    public List<Player> getPlayers() {
        return playerRepository.findAll()
            .stream()
            .toList();
    }

    public Player createPlayer(String name, String email, Long teamId) {
        if (playerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Player with email " + email + " already exists");
        }
        if (teamId != null && !teamService.isExistById(teamId)) {
            throw new IllegalArgumentException("Team with id " + teamId + " does not exist");
        }
        Player player = new Player();
        player.setName(name);
        player.setEmail(email);
        player.setTeamId(teamId);
        return playerRepository.save(player);
    }

    public Player setPlayerTeam(Long playerId, Long teamId) {
        if (!playerRepository.existsById(playerId)) {
            throw new IllegalArgumentException("Player with id " + playerId + " does not exist");
        }
        if (!teamService.isExistById(teamId)) {
            throw new IllegalArgumentException("Team with id " + teamId + " does not exist");
        }
        Player player = playerRepository.findById(playerId).orElse(null);
        player.setTeamId(teamId);
        return playerRepository.save(player);
    }

    public Player getPlayer(Long playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

}
