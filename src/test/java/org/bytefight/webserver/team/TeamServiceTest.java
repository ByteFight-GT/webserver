package org.bytefight.webserver.team;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TeamServiceTest extends FullStackIntegrationTestBase {
    @Autowired
    private TeamService teamService;

    @Autowired
    private TestDataFactory testDataFactory;

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
