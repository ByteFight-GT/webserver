package org.bytefight.webserver.player.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.TeamMemberDetails;
import org.bytefight.webserver.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  boolean existsByUsernameNormalized(String usernameNormalized);

  boolean existsByUsernameNormalizedAndIdIsNot(String usernameNormalized, Long id);

  boolean existsByUserEmail(String email);

  @Query(
      """
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.id = :teamId
    """)
  List<Player> findByTeamId(@Param("teamId") Long teamId);

  @Query(
      """
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.id = :teamId
    """)
  Page<Player> findByTeamId(@Param("teamId") Long teamId, Pageable pageable);

  @Query(
      """
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.id = :teamId
          AND tm.player.id IN :playerIds
    """)
  Page<Player> findByTeamIdAndIdIn(
      @Param("teamId") Long teamId, @Param("playerIds") List<Long> playerIds, Pageable pageable);

  Optional<Player> findByUserUuid(UUID authId);

  Optional<Player> findByUser(User user);

  boolean existsByUserUuid(UUID authId);

  boolean existsByUsername(String username);

  @Query(
      """
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.uuid IN :uuids
    """)
  List<Player> findMembersByTeamUuids(@Param("uuids") List<UUID> uuids);

  @Query(
      """
        SELECT
            tm.team.id AS teamId,
            p.id AS playerId,
            u.id AS playerUserId,
            u.uuid AS playerUuid,
            p.username AS playerUsername,
            u.email AS playerEmail
        FROM TeamMember tm
        JOIN tm.player p
        JOIN p.user u
        WHERE tm.team.id IN :teamIds
        ORDER BY tm.team.id ASC, p.id ASC
    """)
  List<TeamMemberDetails> findMemberDetailsByTeamIds(@Param("teamIds") List<Long> teamIds);

    Optional<Player> findByUsernameNormalized(String usernameNormalized);
}
