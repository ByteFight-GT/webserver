package com.example.botfightwebserver;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.auth.infra.UserRepository;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.competition.infra.CompetitionRepository;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataFactory {
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    public TestDataFactory(CompetitionRepository competitionRepository,
                           TeamRepository teamRepository,
                           UserRepository userRepository,
                           PlayerRepository playerRepository) {
        this.competitionRepository = competitionRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
    }

    public Competition createCompetition() {
        return createCompetition(null, null, true, 2);
    }

    public Competition createCompetition(String slug, String name, boolean active, int maxPlayersPerTeam) {
        Competition competition = new Competition();
        UUID randomUuid = UUID.randomUUID();
        competition.setSlug(slug != null ? slug : "test-competition-" + randomUuid);
        competition.setName(name != null ? name : "Test Competition (" + randomUuid + ")");
        competition.setActive(active);
        competition.setMaxPlayersPerTeam(maxPlayersPerTeam);
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

    public User createUser() {
        return createUser(null, false);
    }

    public User createUser(String email, boolean isAdmin) {
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail(email != null ? email : "user-" + UUID.randomUUID() + "@example.com");
        user.setAdmin(isAdmin);
        return userRepository.save(user);
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
        player.setUsername_normalized(resolvedUsername.toLowerCase());
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
