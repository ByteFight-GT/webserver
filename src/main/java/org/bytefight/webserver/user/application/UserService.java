package org.bytefight.webserver.user.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.bytefight.webserver.auth.application.AuthService;
import org.bytefight.webserver.auth.domain.EmailAlreadyRegisteredException;
import org.bytefight.webserver.auth.domain.RegistrationException;
import org.bytefight.webserver.auth.domain.UsernameAlreadyExistsException;
import org.bytefight.webserver.auth.domain.dto.SupabaseDtos;
import org.bytefight.webserver.auth.infra.SignupProperties;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.RegisterUserDto;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final PlayerService playerService;
  private final AuthService authService;
  private final LocalStorageService storageService;
  private final LocalStorageService localStorageService;

  private final SignupProperties signupProperties;

  static String normalize(String raw) {
    return raw == null ? null : raw.trim().toLowerCase();
  }

  @Transactional
  public User createFromJwt(Jwt jwt) {
    UUID uuid = UUID.fromString(jwt.getSubject());
    String email = (String) jwt.getClaims().get("email");

    User u = new User();
    u.setUuid(uuid);
    u.setEmail(email.toLowerCase());
    return userRepository.save(u);
  }

  public Optional<User> findByUuid(String uuid) {
    return userRepository.findByUuid(UUID.fromString(uuid));
  }

  @Transactional
  public User signup(RegisterUserDto input) {
    return signup(input, false);
  }

  @Transactional
  public User signup(RegisterUserDto input, boolean confirmEmail) {
    String normalizedEmail = normalize(input.getEmail());
    String normalizedUsername = normalize(input.getName());

    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new EmailAlreadyRegisteredException(input.getEmail());
    }

    if (playerRepository.existsByUsernameNormalized(normalizedUsername)) {
      throw new UsernameAlreadyExistsException(input.getName());
    }

    if (!signupProperties.isEmailAllowed(normalizedEmail)) {
      throw new RegistrationException("Sorry, ByteFight isn't available for your institution yet.");
    }

    try {
      User user = new User();
      user.setUuid(
          UUID.randomUUID()); // Set this to a random UUID first, then overwrite with UUID from
      // Supabase
      user.setEmail(normalizedEmail);
      user.setLastAcceptedTos(Instant.now());
      user = userRepository.save(user);

      playerService.createPlayer(user, input.getName());

      SupabaseDtos.SupabaseUser supabaseUser =
          authService.createUser(
              normalizedEmail, input.getPassword(), confirmEmail, Map.of(), Map.of());

      user.setUuid(UUID.fromString(supabaseUser.id()));
      user = userRepository.save(user);

      return user;
    } catch (AuthService.AuthServiceException e) {
      throw new RegistrationException(e.getMessage());
    }
  }

  @Transactional
  public FileRecord uploadResume(MultipartFile file, UUID userUuid) throws IOException {
    User user = userRepository.findByUuid(userUuid).orElseThrow();
    FileRecord oldResume = user.getResume();

    if (file == null || file.isEmpty()) {
      throw new IOException("No resume is uploaded");
    }

    String fileName = file.getOriginalFilename();

    if (fileName == null) {
      throw new IOException("File name is null");
    }

    String lower = fileName.toLowerCase();

    Set<String> allowed_type = Set.of(".pdf");

    boolean valid = allowed_type.stream().anyMatch(lower::endsWith);

    if (!valid) {
      throw new IllegalArgumentException("Please submit your resume in PDF, DOC, or DOCX format");
    }

    FileRecord newResume =
        storageService.store(file, "resumes/" + user.getUuid(), fileName, true, 2_097_152);

    if (oldResume != null) {
      storageService.delete(oldResume.getUuid().toString());
    }

    user.setResume(newResume);
    userRepository.save(user);

    return newResume;
  }

  @Transactional
  public void deleteResume(UUID userUuid) {
    User user = userRepository.findByUuid(userUuid).orElseThrow();
    FileRecord resume = user.getResume();

    if (resume == null) {
      throw new NoSuchElementException("No resume found");
    }

    user.setResume(null);
    userRepository.save(user);
    storageService.delete(resume.getUuid().toString());
  }

  public ResumeDto getResume(UUID userUuid) {
    User user = userRepository.findByUuid(userUuid).orElseThrow();
    FileRecord resume = user.getResume();

    if (resume == null) {
      throw new NoSuchElementException("No resume found");
    }

    DownloadLinkDto link =
        localStorageService.getDownloadLink(resume.getUuid().toString(), Duration.ofMinutes(5));
    return ResumeDto.from(link, user);
  }

  public void acceptTos(UUID userUuid) {
    User user = userRepository.findByUuid(userUuid).orElseThrow();
    user.setLastAcceptedTos(Instant.now());
    userRepository.save(user);
  }
}
