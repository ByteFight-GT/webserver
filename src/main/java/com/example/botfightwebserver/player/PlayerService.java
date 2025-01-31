package com.example.botfightwebserver.player;
;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public List<Player> getPlayers() {
        return playerRepository.findAll()
            .stream()
            .toList();
    }

    public Player createPlayer(String name, String email, UUID authId, Long teamId) {
        if (playerRepository.existsByEmail(email)) {throw new IllegalArgumentException("Player with email " + email + " already exists");
        }
        Player player = new Player();
        player.setName(name);
        player.setEmail(email);
        player.setTeamId(teamId);
        player.setAuthId(authId);
        return playerRepository.save(player);
    }

    public Player setPlayerTeam(UUID playerId, Long teamId) {
        if (!playerRepository.existsByAuthId(playerId)) {
            throw new IllegalArgumentException("Player with id " + playerId + " does not exist");
        }
        Player player = playerRepository.findByAuthId(playerId).orElse(null);
        player.setTeamId(teamId);
        player.setHasTeam(true);
        return playerRepository.save(player);
    }

    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    public Player getPlayer(Long playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

    public Player getPlayer(UUID authId) {
        return playerRepository.findByAuthId(authId).orElse(null);
    }

    public boolean isUsernameExist(String username) {
        return playerRepository.existsByName(username);
    }

}
