package org.bytefight.webserver.player.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.UsernameAlreadyExistsException;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.domain.dto.UpdatePlayerProfileDto;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.user.domain.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;

  public Player getPlayer(Long playerId) {
    if (playerId == null) {
      throw new IllegalArgumentException("Player id cannot be null");
    }
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new IllegalArgumentException("Player not found"));
  }

  public Optional<Player> getPlayerByUsername(String username) {
    if (username == null) {
      throw new IllegalArgumentException();
    }
    String normalized = normalizeUsername(username);
    return playerRepository.findByUsernameNormalized(normalized);
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

  private void applyUsername(Player player, String username) {
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    String normalized = normalizeUsername(username);
    if (normalized == null || normalized.isBlank()) {
      throw new IllegalArgumentException("Username cannot be blank");
    }
    String currentNormalized = normalizeUsername(player.getUsername());
    if (!normalized.equals(currentNormalized)
        && playerRepository.existsByUsernameNormalizedAndIdIsNot(normalized, player.getId())) {
      throw new UsernameAlreadyExistsException(username);
    }
    player.setUsername(username.trim());
    player.setUsernameNormalized(normalized);
  }

  public void setUsername(Player player, String username) {
    applyUsername(player, username);
    playerRepository.save(player);
  }

  @Transactional
  public Player updateProfile(Player player, UpdatePlayerProfileDto input) {
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    if (input == null) {
      throw new IllegalArgumentException("Profile input cannot be null");
    }

    if (input.getUsername() != null) {
      applyUsername(player, input.getUsername());
    }
    if (input.getFullName() != null) {
      player.setFullName(input.getFullName());
    }
    if (input.getDescription() != null) {
      player.setDescription(input.getDescription());
    }
    if (input.getSchool() != null) {
      player.setSchool(input.getSchool());
    }
    if (input.getMajor() != null) {
      player.setMajor(input.getMajor());
    }
    if (input.getGithubLink() != null) {
      player.setGithubLink(input.getGithubLink());
    }
    if (input.getLinkedinLink() != null) {
      player.setLinkedinLink(input.getLinkedinLink());
    }
    if (input.getWebsiteLink() != null) {
      player.setWebsiteLink(input.getWebsiteLink());
    }

    return playerRepository.save(player);
  }

  @Transactional
  public Player createPlayer(User user, String name) {
    if (user == null || name == null) {
      throw new IllegalArgumentException("User or name cannot be null");
    }

    Player player = new Player();
    player.setUser(user);
    setUsername(player, name);

    return playerRepository.save(player);
  }

  public List<Player> getPlayers() {
    return playerRepository.findAll().stream().toList();
  }
}
