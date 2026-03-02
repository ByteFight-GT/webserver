package org.bytefight.webserver.team.infra;

import java.util.List;
import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.leaderboard.domain.MemberSummary;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
  boolean existsByCompetitionAndPlayerAndTeamIsDeletedFalse(Competition competition, Player player);

  Optional<TeamMember> findByCompetitionAndPlayerAndTeamIsDeletedFalse(
      Competition competition, Player player);

  boolean existsByTeamAndPlayerAndTeamIsDeletedFalse(Team team, Player player);

    List<TeamMember> findByTeam(Team team);
    long countByTeam(Team team);
    @Query("""
        SELECT COUNT(tm.player.id) as playerCount
        FROM TeamMember tm
        JOIN tm.team t
        WHERE t.competition.id = :competitionId
          AND t.isDeleted = false
    """)
    long countPlayersByCompetitionId(@Param("competitionId") Long competitionId);

  @Query(
      value =
          """
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
        """,
      nativeQuery = true)
  List<MemberSummary> findMemberSummariesByTeamUuids(@Param("teamUuids") List<String> teamUuids);
}
