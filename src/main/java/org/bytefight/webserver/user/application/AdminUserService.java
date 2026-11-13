package org.bytefight.webserver.user.application;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.AdminUpdateUserDto;
import org.bytefight.webserver.user.domain.dto.AdminUserDetailDto;
import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto;
import org.bytefight.webserver.user.domain.dto.EmailDomainCountDto;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
  private final UserRepository userRepository;
  private final LocalStorageService localStorageService;
  private final PlayerRepository playerRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final PlayerService playerService;

  public AdminUserService(
      UserRepository userRepository,
      LocalStorageService localStorageService,
      PlayerRepository playerRepository,
      TeamMemberRepository teamMemberRepository,
      PlayerService playerService) {
    this.userRepository = userRepository;
    this.localStorageService = localStorageService;
    this.playerRepository = playerRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.playerService = playerService;
  }

  /**
   * Full detail for one user: account flags, player profile, resume metadata, and current
   * (non-deleted) team memberships.
   *
   * @throws NoSuchElementException if no user has this id
   */
  @Transactional(readOnly = true)
  public AdminUserDetailDto getUserDetail(long id) {
    User user = userRepository.findById(id).orElseThrow();
    Player player = playerRepository.findByUser(user).orElse(null);
    List<TeamMember> memberships =
        player != null ? teamMemberRepository.findByPlayerAndTeamDeletedAtNull(player) : List.of();
    return AdminUserDetailDto.from(user, player, memberships);
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


  /**
   * Applies an admin edit and returns the updated detail. Username changes go through {@link
   * PlayerService#setUsername} so normalization and the uniqueness check stay in one place.
   *
   * @throws NoSuchElementException if no user has this id
   * @throws IllegalArgumentException if profile fields are sent for a user with no player
   */
  @Transactional
  public AdminUserDetailDto updateUser(long id, AdminUpdateUserDto input) {
    User user = userRepository.findById(id).orElseThrow();
    if (input.serviceAccount() != null) {
      user.setServiceAccount(input.serviceAccount());
    }
    if (input.internalCompEligible() != null) {
      user.setInternalCompEligible(input.internalCompEligible());
    }
    userRepository.save(user);

    if (input.player() != null) {
      Player player =
          playerRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new IllegalArgumentException("This user has no player profile to edit"));
      AdminUpdateUserDto.PlayerUpdate p = input.player();
      player.setFullName(blankToNull(p.fullName()));
      player.setDescription(blankToNull(p.description()));
      player.setSchool(blankToNull(p.school()));
      player.setMajor(blankToNull(p.major()));
      player.setGithubLink(blankToNull(p.githubLink()));
      player.setLinkedinLink(blankToNull(p.linkedinLink()));
      player.setWebsiteLink(blankToNull(p.websiteLink()));
      if (p.dev() != null) {
        player.setDev(p.dev());
      }
      playerService.setUsername(player, p.username());
    }
    return getUserDetail(id);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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
