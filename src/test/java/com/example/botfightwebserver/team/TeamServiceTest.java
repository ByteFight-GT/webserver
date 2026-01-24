package com.example.botfightwebserver.team;

import com.example.botfightwebserver.FullStackIntegrationTestBase;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TeamServiceTest extends FullStackIntegrationTestBase {
    private final TeamService teamService;
    private final TestDataFactory testDataFactory;

    @Autowired
    TeamServiceTest(TeamService teamService, TestDataFactory testDataFactory) {
        this.teamService = teamService;
        this.testDataFactory = testDataFactory;
    }

    @Test
    void getTeamByUuidReturnsTeamWhenActive() {
        Competition competition = testDataFactory.createCompetition();
        UUID teamUuid = UUID.randomUUID();
        testDataFactory.createTeam(competition, teamUuid, false);

        Optional<Team> result = teamService.getTeamByUuid(teamUuid);

        assertThat(result).isPresent();
        assertThat(result.get().getUuid()).isEqualTo(teamUuid);
    }

    @Test
    void getTeamByUuidReturnsEmptyWhenDeleted() {
        Competition competition = testDataFactory.createCompetition();
        UUID teamUuid = UUID.randomUUID();
        testDataFactory.createTeam(competition, teamUuid, true);

        Optional<Team> result = teamService.getTeamByUuid(teamUuid);

        assertThat(result).isEmpty();
    }

}
