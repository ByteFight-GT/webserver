package org.bytefight.webserver.team.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.leaderboard.domain.MemberSummary;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
        SELECT
            t.uuid::text AS teamUuid,
            u.uuid::text AS uuid,
            p.username AS username
        FROM team_members tm
        JOIN teams t ON t.id = tm.team_id
        JOIN players p ON p.id = tm.player_id
        JOIN users u ON u.id = p.user_id
        WHERE t.uuid::text IN (:teamUuids)
        ORDER BY t.id ASC, p.username ASC
        """, nativeQuery = true)
    List<MemberSummary> findMemberSummariesByTeamUuids(@Param("teamUuids") List<String> teamUuids);
}
