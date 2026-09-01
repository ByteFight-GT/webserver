package org.bytefight.webserver;

import java.time.Instant;
import java.util.UUID;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.competition.infra.CompetitionRepository;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.bytefight.webserver.ladder.infra.LadderRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class TestDataFactory {
  private final CompetitionRepository competitionRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final LadderRepository ladderRepository;

  public TestDataFactory(
      CompetitionRepository competitionRepository,
      TeamRepository teamRepository,
      UserRepository userRepository,
      PlayerRepository playerRepository,
      LadderRepository ladderRepository) {
    this.competitionRepository = competitionRepository;
    this.teamRepository = teamRepository;
    this.userRepository = userRepository;
    this.playerRepository = playerRepository;
    this.ladderRepository = ladderRepository;
  }

  public Competition createCompetition() {
    return createCompetition(null, null, true, 2);
  }

  public Competition createCompetition(
      String slug, String name, boolean active, int maxPlayersPerTeam) {
    Competition competition = new Competition();
    UUID randomUuid = UUID.randomUUID();
    competition.setSlug(slug != null ? slug : "test-competition-" + randomUuid);
    competition.setName(name != null ? name : "Test Competition (" + randomUuid + ")");
    competition.setActive(active);
    competition.setMaxPlayersPerTeam(maxPlayersPerTeam);
    competition.setAllowCreateTeam(true);
    competition.setAllowJoinTeam(true);
    competition.setAllowLeaveTeam(true);
    competition.setAllowEditTeamName(true);
    competition.setAllowNewSubmission(true);
    competition.setAllowSetSubmission(true);
    competition.setTeamSubmissionStorageSize(200 * 1000 * 1000);
    return competitionRepository.save(competition);
  }

  public Competition createInternalCompetition(String slug) {
    Competition competition = createCompetition(slug, null, true, 2);
    competition.setInternal(true);
    return competitionRepository.save(competition);
  }

  public Team createTeam(Competition competition) {
    return createTeam(competition, UUID.randomUUID(), false);
  }

  public Team createTeam(Competition competition, UUID uuid, boolean deleted) {
    Team team = new Team();
    team.setCompetition(competition);
    team.setUuid(uuid != null ? uuid : UUID.randomUUID());
    team.setName("Team " + team.getUuid().toString().substring(0, 8));
    team.setDisplayMembers(false);
    team.setJoinCode(UUID.randomUUID().toString());

    if (deleted) {
      team.softDelete();
    }
    return teamRepository.save(team);
  }

  public Ladder createLadder(Competition competition, String ladderSlug) {
    Ladder ladder = new Ladder();
    ladder.setCompetition(competition);
    ladder.setLadder(ladderSlug);
    ladder.setGlickoDefaultRating(1500.0);
    ladder.setGlickoDefaultRd(350.0);
    ladder.setGlickoRdMax(350.0);
    ladder.setGlickoRdMin(30.0);
    ladder.setGlickoPhiInflationPerDay(0.0);
    ladder.setGlickoTau(0.5);
    ladder.setGlickoSigmaDefault(0.06);
    ladder.setGlickoSigmaMin(0.03);
    ladder.setGlickoSigmaMax(0.2);
    return ladderRepository.save(ladder);
  }

  public User createUser() {
    return createUser(null, false);
  }

  public User createUser(String email, boolean isAdmin) {
    return createUser(email, isAdmin, false);
  }

  public User createUser(String email, boolean isAdmin, boolean isServiceAccount) {
    User user = new User();
    user.setUuid(UUID.randomUUID());
    user.setEmail(email != null ? email : "user-" + UUID.randomUUID() + "@example.com");
    user.setAdmin(isAdmin);
    user.setServiceAccount(isServiceAccount);
    user.setLastAcceptedTos(Instant.now());
    return userRepository.save(user);
  }

  public User createServiceAccount() {
    return createServiceAccount(null);
  }

  public User createServiceAccount(String email) {
    return createUser(email, false, true);
  }

  public Player createPlayer(User user) {
    return createPlayer(user, null);
  }

  public Player createPlayer(User user, String username) {
    if (user == null) {
      throw new IllegalArgumentException("User is required");
    }
    Player player = new Player();
    player.setUser(user);
    String resolvedUsername = username != null ? username : "player-" + UUID.randomUUID();
    player.setUsername(resolvedUsername);
    player.setUsernameNormalized(resolvedUsername.trim().toLowerCase());
    return playerRepository.save(player);
  }

  public Player createUserWithPlayer() {
    return createUserWithPlayer(null, null);
  }

  public Player createUserWithPlayer(String email, String username) {
    User user = createUser(email, false);
    return createPlayer(user, username);
  }
}
