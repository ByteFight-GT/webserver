package com.example.botfightwebserver.player.application;
import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.auth.domain.UsernameAlreadyExistsException;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PermissionsService permissionsService;
    private final TeamRepository teamRepository;

    public Player getPlayer(Long playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id cannot be null");
        }
        return playerRepository.findById(playerId).orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public Optional<Player> getPlayer(UUID authId) {
        if (authId == null) {
            throw new IllegalArgumentException("Auth id cannot be null");
        }
        return playerRepository.findByUserUuid(authId);
    }

    public Optional<Player> getPlayer(User user) {
        return playerRepository.findByUser(user);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    public boolean isUsernameExist(String username) {
        String normalized = normalizeUsername(username);
        return normalized != null && playerRepository.existsByUsernameNormalized(normalized);
    }

    public void setUsername(Player player, String username) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        String normalized = normalizeUsername(username);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        String currentNormalized = normalizeUsername(player.getUsername());
        if (!normalized.equals(currentNormalized) && playerRepository.existsByUsernameNormalizedAndIdIsNot(normalized, player.getId())) {
            throw new UsernameAlreadyExistsException(username);
        }
        player.setUsername(username.trim());
        player.setUsernameNormalized(normalized);
        playerRepository.save(player);
    }

    @Transactional
    public Player createPlayer(User user, String name) {
        if(user == null || name == null) {
            throw new IllegalArgumentException("User or name cannot be null");
        }

        Player player = new Player();
        player.setUser(user);
        setUsername(player, name);

        return playerRepository.save(player);
    }

    public List<Player> getPlayers() {
        return playerRepository.findAll()
            .stream()
            .toList();
    }
}
