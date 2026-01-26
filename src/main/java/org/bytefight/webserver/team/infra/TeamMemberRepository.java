package org.bytefight.webserver.team.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    boolean existsByCompetitionAndPlayer(Competition competition, Player player);
    boolean existsByTeamAndPlayer(Team team, Player player);
    Optional<TeamMember> findByCompetitionAndPlayer(Competition competition, Player player);
    List<TeamMember> findByTeam(Team team);
    long countByTeam(Team team);
}
