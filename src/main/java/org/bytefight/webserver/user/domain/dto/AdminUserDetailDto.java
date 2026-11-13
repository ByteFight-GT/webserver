package org.bytefight.webserver.user.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.user.domain.User;

/** Everything the admin panel shows on a single user's page. */
public record AdminUserDetailDto(
    Long id,
    UUID uuid,
    String email,
    Instant createdAt,
    Instant updatedAt,
    boolean admin,
    boolean serviceAccount,
    boolean internalCompEligible,
    Instant lastAcceptedTos,
    PlayerInfo player,
    ResumeInfo resume,
    List<TeamInfo> teams) {

  public record PlayerInfo(
      Long id,
      String username,
      String fullName,
      String description,
      String school,
      String major,
      String githubLink,
      String linkedinLink,
      String websiteLink,
      boolean dev,
      Instant createdAt,
      Instant updatedAt) {
    static PlayerInfo from(Player p) {
      return new PlayerInfo(
          p.getId(),
          p.getUsername(),
          p.getFullName(),
          p.getDescription(),
          p.getSchool(),
          p.getMajor(),
          p.getGithubLink(),
          p.getLinkedinLink(),
          p.getWebsiteLink(),
          p.isDev(),
          p.getCreatedAt(),
          p.getUpdatedAt());
    }
  }

  public record ResumeInfo(
      UUID uuid, String filename, String contentType, Long size, Instant uploadedAt) {
    static ResumeInfo from(FileRecord f) {
      return new ResumeInfo(
          f.getUuid(), f.getFilename(), f.getContentType(), f.getSize(), f.getCreatedAt());
    }
  }

  public record TeamInfo(
      Long teamId,
      UUID teamUuid,
      String teamName,
      TeamType teamType,
      Long competitionId,
      String competitionName,
      Instant joinedAt) {
    static TeamInfo from(TeamMember m) {
      return new TeamInfo(
          m.getTeam().getId(),
          m.getTeam().getUuid(),
          m.getTeam().getName(),
          m.getTeam().getType(),
          m.getCompetition().getId(),
          m.getCompetition().getName(),
          m.getCreatedAt());
    }
  }

  public static AdminUserDetailDto from(User u, Player player, List<TeamMember> memberships) {
    return new AdminUserDetailDto(
        u.getId(),
        u.getUuid(),
        u.getEmail(),
        u.getCreatedAt(),
        u.getUpdatedAt(),
        u.isAdmin(),
        u.isServiceAccount(),
        u.isInternalCompEligible(),
        u.getLastAcceptedTos(),
        player != null ? PlayerInfo.from(player) : null,
        u.getResume() != null ? ResumeInfo.from(u.getResume()) : null,
        memberships.stream().map(TeamInfo::from).toList());
  }
}
