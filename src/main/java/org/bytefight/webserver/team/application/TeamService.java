package org.bytefight.webserver.team.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bytefight.webserver.common.domain.PermissionDeniedException;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.application.TeamStatsService;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.team.domain.dto.TeamSettingsDto;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.whitelist.application.WhitelistService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {
  private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int JOIN_CODE_LENGTH = 8;
  private static final int JOIN_CODE_MAX_ATTEMPTS = 10;

  private final LadderRepository ladderRepository;
  private final TeamStatsService teamStatsService;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final WhitelistService whitelistService;

  public String generateJoinCode() {
    for (int attempt = 0; attempt < JOIN_CODE_MAX_ATTEMPTS; attempt++) {
      String code = generateJoinCodeCandidate();
      if (!teamRepository.existsByJoinCode(code)) {
        return code;
      }
    }

    throw new IllegalStateException(
        "Failed to generate unique join code after " + JOIN_CODE_MAX_ATTEMPTS + " attempts");
  }

  private String generateJoinCodeCandidate() {
    StringBuilder builder = new StringBuilder(JOIN_CODE_LENGTH);
    for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
      int index = ThreadLocalRandom.current().nextInt(JOIN_CODE_ALPHABET.length());
      builder.append(JOIN_CODE_ALPHABET.charAt(index));
    }
    return builder.toString();
  }

  public Optional<Team> findTeamByCompetitionAndPlayer(Competition competition, Player player) {
    return teamMemberRepository
        .findByCompetitionAndPlayerAndTeamDeletedAtNull(competition, player)
        .map(TeamMember::getTeam);
  }

  public Team createTeam(Competition competition, TeamSettingsDto teamSettingsDto) {
    return createTeam(competition, teamSettingsDto, true);
  }

  public Team createTeam(
      Competition competition, TeamSettingsDto teamSettingsDto, boolean requireActive) {
    if (!competition.isActive() && requireActive) {
      throw new IllegalArgumentException("Competition is not active");
    }

    String name = teamSettingsDto.getName();

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Team name cannot be null or empty");
    }

    String normalizedName = name.trim().toLowerCase();

    if (teamRepository.existsByCompetitionAndNameNormalized(competition, normalizedName)) {
      throw new IllegalArgumentException("Team with name " + name + " already exists");
    }

    String joinCode = generateJoinCode();

    Team team = new Team();
    team.setUuid(UUID.randomUUID());
    team.setType(TeamType.regular);
    team.setCompetition(competition);
    team.setName(name);
    team.setDisplayMembers(teamSettingsDto.getDisplayMembers());
    team.setJoinCode(joinCode);

    if (teamSettingsDto.getQuote() != null) {
      team.setQuote(teamSettingsDto.getQuote());
    }

    Team savedTeam = teamRepository.save(team);

    List<Ladder> ladders = ladderRepository.findAllByCompetition(competition);

    for (Ladder ladder : ladders) {
      teamStatsService.getTeamStatsCreateIfNotExist(savedTeam, ladder.getLadder());
    }

    return savedTeam;
  }

  public TeamMember joinTeam(Player player, Team team) {
    if (player == null || team == null) {
      throw new IllegalArgumentException("Player and team are required");
    }

    Competition competition = team.getCompetition();
    if (competition == null) {
      throw new IllegalArgumentException("Team must belong to a competition");
    }

    if (!competition.isActive()) {
      throw new IllegalArgumentException("Competition is not active");
    }

    if (!whitelistService.isCompetitionParticipationAllowed(competition, player.getUser())) {
      throw new IllegalArgumentException(
          "You must be whitelisted to participate in this competition");
    }

    if (teamMemberRepository.existsByCompetitionAndPlayerAndTeamDeletedAtNull(
        competition, player)) {
      throw new IllegalArgumentException("Player is already in a team for this competition");
    }

    if (countPlayersForTeam(team) >= competition.getMaxPlayersPerTeam()) {
      throw new IllegalArgumentException("Team is full");
    }

    TeamMember member = new TeamMember();
    member.setPlayer(player);
    member.setTeam(team);
    member.setCompetition(competition);
    return teamMemberRepository.save(member);
  }

  public void leaveTeam(Competition competition, Player player) {
    if (player == null || competition == null) {
      throw new IllegalArgumentException("Competition and player are required");
    }

    if (!competition.isActive()) {
      throw new IllegalArgumentException("Competition is not active");
    }

    TeamMember member =
        teamMemberRepository
            .findByCompetitionAndPlayerAndTeamDeletedAtNull(competition, player)
            .orElseThrow(
                () -> new IllegalArgumentException("Player is not in a team for this competition"));

    Team team = member.getTeam();

    teamMemberRepository.delete(member);

    if (countPlayersForTeam(team) == 0) {
      team.softDelete();
      teamRepository.save(team);
    }
  }

  public TeamMember joinTeamByJoinCode(Competition competition, Player player, String joinCode) {
    Team team =
        teamRepository
            .findByCompetitionAndJoinCodeAndDeletedAtNull(competition, joinCode)
            .orElseThrow(
                () -> new IllegalArgumentException("A team with that join code was no found"));
    return joinTeam(player, team);
  }

  public Optional<Team> getTeamByUuid(UUID uuid) {
    return teamRepository.findByUuidAndDeletedAtNull(uuid);
  }

  public List<Player> getPlayersForTeam(Team team) {
    if (team == null || team.getId() == null) {
      throw new IllegalArgumentException("Team is required");
    }

    return teamMemberRepository.findByTeam(team).stream().map(TeamMember::getPlayer).toList();
  }

  public boolean isMember(Team team, Player player) {
    if (team == null || player == null) {
      throw new IllegalArgumentException("Team and player are required");
    }

    return teamMemberRepository.existsByTeamAndPlayerAndTeamDeletedAtNull(team, player);
  }

  public long countPlayersForTeam(Team team) {
    if (team == null || team.getId() == null) {
      throw new IllegalArgumentException("Team is required");
    }

    return teamMemberRepository.countByTeam(team);
  }

  public Optional<Team> getTeamByCompetitionAndUuid(Competition competition, UUID uuid) {
    return teamRepository.findByCompetitionAndUuid(competition, uuid);
  }

  public Optional<Team> getTeamByCompetitionAndName(Competition competition, String name) {
    return teamRepository.findByCompetitionAndNameNormalizedAndDeletedAtNull(
        competition, name.trim().toLowerCase());
  }

  /**
   * Acquire a row lock on the team for the current transaction, serializing a check-then-act (e.g.
   * the user-match rate limiter, #112) against concurrent requests for the same team.
   */
  public void lockTeamForUpdate(Long teamId) {
    teamRepository.findByIdForUpdate(teamId);
  }

  public List<Team> getTeamsWithSubmission(Competition competition) {
    return teamRepository.findAllByDeletedAtNullAndCurrentSubmissionIsNotNullAndCompetition(
        competition);
  }

  public void setTeamCurrentSubmission(Team team, Submission submission) {
    if (submission.getValidity() != SubmissionValidity.valid) {
      throw new IllegalArgumentException("Submission is invalid");
    }

    if (!team.equals(submission.getTeam())) {
      throw new IllegalArgumentException("Submission does not belong to team");
    }

    team.setCurrentSubmission(submission);

    teamRepository.save(team);
  }

  public Team getTeamById(Long id) {
    return teamRepository.getReferenceById(id);
  }

  public Optional<Submission> getCurrentSubmission(Long teamId) {
    Optional<Submission> submission =
        teamRepository.findById(teamId).map(Team::getCurrentSubmission);
    return submission;
  }

  @Transactional
  public void editTeam(Team team, TeamSettingsDto teamSettingsDto) {
    if (team == null) {
      throw new IllegalArgumentException("Team is required");
    }

    if (team.isDeleted()) {
      throw new IllegalArgumentException("This team is deleted and cannot be edited.");
    }

    if (teamSettingsDto.getName() != null && !teamSettingsDto.getName().equals(team.getName())) {
      if (!team.getCompetition().isAllowEditTeamName()) {
        throw new PermissionDeniedException("You may not edit your team's name at this time");
      }

      String normalizedName = teamSettingsDto.getName().trim().toLowerCase();
      if (!normalizedName.equals(team.getNameNormalized())
          && teamRepository.existsByCompetitionAndNameNormalized(
              team.getCompetition(), normalizedName)) {
        throw new IllegalArgumentException(
            "Team with name " + teamSettingsDto.getName() + " already exists");
      }
      team.setName(teamSettingsDto.getName());
    }

    if (teamSettingsDto.getQuote() != null) team.setQuote(teamSettingsDto.getQuote());
    if (teamSettingsDto.getDisplayMembers() != null)
      team.setDisplayMembers(teamSettingsDto.getDisplayMembers());

    teamRepository.save(team);
  }

  public int countTeamsWithSubmission() {
    return teamRepository.countByCurrentSubmissionNotNull();
  }
}
