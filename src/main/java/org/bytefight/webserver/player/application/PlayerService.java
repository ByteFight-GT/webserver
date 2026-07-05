package org.bytefight.webserver.player.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bytefight.webserver.auth.domain.UsernameAlreadyExistsException;
import org.bytefight.webserver.player.domain.AvatarDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private static final long MAX_AVATAR_BYTES = 5_242_880L; // 5 MiB
  private static final Set<String> ALLOWED_AVATAR_EXTENSIONS =
      Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final LocalStorageService storageService;

  public Player getPlayer(Long playerId) {
    if (playerId == null) {
      throw new IllegalArgumentException("Player id cannot be null");
    }
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new IllegalArgumentException("Player not found"));
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
    if (!normalized.equals(currentNormalized)
        && playerRepository.existsByUsernameNormalizedAndIdIsNot(normalized, player.getId())) {
      throw new UsernameAlreadyExistsException(username);
    }
    player.setUsername(username.trim());
    player.setUsernameNormalized(normalized);
    playerRepository.save(player);
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

  @Transactional
  public FileRecord uploadAvatar(MultipartFile file, UUID authId) throws IOException {
    Player player = playerRepository.findByUserUuid(authId).orElseThrow();
    FileRecord oldAvatar = player.getAvatar();

    if (file == null || file.isEmpty()) {
      throw new IOException("No avatar is uploaded");
    }

    String fileName = file.getOriginalFilename();

    if (fileName == null) {
      throw new IOException("File name is null");
    }

    String lower = fileName.toLowerCase();
    boolean valid = ALLOWED_AVATAR_EXTENSIONS.stream().anyMatch(lower::endsWith);

    if (!valid) {
      throw new IllegalArgumentException(
          "Please submit your avatar in PNG, JPG, JPEG, WEBP, or GIF format");
    }

    FileRecord newAvatar =
        storageService.store(
            file,
            "avatars/" + player.getUser().getUuid(),
            fileName,
            false,
            MAX_AVATAR_BYTES);

    if (oldAvatar != null) {
      storageService.delete(oldAvatar.getUuid().toString());
    }

    player.setAvatar(newAvatar);
    playerRepository.save(player);

    return newAvatar;
  }

  @Transactional
  public void deleteAvatar(UUID authId) {
    Player player = playerRepository.findByUserUuid(authId).orElseThrow();
    FileRecord avatar = player.getAvatar();

    if (avatar == null) {
      throw new NoSuchElementException("No avatar found");
    }

    player.setAvatar(null);
    playerRepository.save(player);
    storageService.delete(avatar.getUuid().toString());
  }

  @Transactional
  public AvatarDto getAvatar(UUID authId) {
    Player player = playerRepository.findByUserUuid(authId).orElseThrow();
    FileRecord avatar = player.getAvatar();

    if (avatar == null) {
      throw new NoSuchElementException("No avatar found");
    }

    DownloadLinkDto link =
        storageService.getDownloadLink(avatar.getUuid().toString(), Duration.ofMinutes(5));
    return AvatarDto.from(link, player);
  }
}
