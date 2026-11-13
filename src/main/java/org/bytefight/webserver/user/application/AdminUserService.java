package org.bytefight.webserver.user.application;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto;
import org.bytefight.webserver.user.domain.dto.EmailDomainCountDto;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
  private final UserRepository userRepository;
  private final LocalStorageService localStorageService;

  public AdminUserService(UserRepository userRepository, LocalStorageService localStorageService) {
    this.userRepository = userRepository;
    this.localStorageService = localStorageService;
  }

  /**
   * @param query case-insensitive substring of email or username, or null
   * @param emailDomain email domain such as {@code gatech.edu} (a leading {@code @} is allowed),
   *     matched exactly and case-insensitively, or null
   */
  public Page<AdminUserWithPlayerAndResumeDto> listUsers(
      Pageable pageable, List<Long> userIds, String query, String emailDomain) {
    if (userIds != null && !userIds.isEmpty()) {
      return userRepository.findAllWithPlayersByIdIn(userIds, pageable);
    }
    String pattern = isBlank(query) ? "%" : "%" + escapeLike(normalize(query)) + "%";
    String domain = isBlank(emailDomain) ? "" : normalize(emailDomain).replaceFirst("^@+", "");
    String emailPattern = domain.isEmpty() ? "%" : "%@" + escapeLike(domain);
    if (pattern.equals("%") && emailPattern.equals("%")) {
      return userRepository.findAllWithPlayers(pageable);
    }
    return userRepository.searchWithPlayers(pattern, emailPattern, pageable);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private static String escapeLike(String value) {
    return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }


  public List<EmailDomainCountDto> listEmailDomains() {
    return userRepository.countUsersByEmailDomain().stream()
        .map(row -> new EmailDomainCountDto((String) row[0], ((Number) row[1]).longValue()))
        .toList();
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
}
