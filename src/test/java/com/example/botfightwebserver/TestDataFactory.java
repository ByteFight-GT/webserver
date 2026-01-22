package com.example.botfightwebserver;

import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.competition.infra.CompetitionRepository;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataFactory {
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;

    public TestDataFactory(CompetitionRepository competitionRepository, TeamRepository teamRepository) {
        this.competitionRepository = competitionRepository;
        this.teamRepository = teamRepository;
    }

    public Competition createCompetition() {
        return createCompetition(null, "Test Competition", true);
    }

    public Competition createCompetition(String slug, String name, boolean active) {
        Competition competition = new Competition();
        competition.setSlug(slug != null ? slug : "test-competition-" + UUID.randomUUID());
        competition.setName(name != null ? name : "Test Competition");
        competition.setActive(active);
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
        if (deleted) {
            team.softDelete();
        }
        return teamRepository.save(team);
    }
}
