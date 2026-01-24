package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.player.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    @Query("""
        SELECT (COUNT(p) > 0)
        FROM Player p
        WHERE lower(p.username) = lower(:name)
    """)
    boolean existsByUsernameIgnoreCase(@Param("name") String name);
    boolean existsByUserEmail(String email);
    @Query("""
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.id = :teamId
    """)
    List<Player> findByTeamId(@Param("teamId") Long teamId);
    Optional<Player> findByUserUuid(UUID authId);
    Optional<Player> findByUser(User user);
    boolean existsByUserUuid(UUID authId);
    boolean existsByUsername(String username);

    @Query("""
        SELECT tm.player
        FROM TeamMember tm
        WHERE tm.team.uuid IN :uuids
    """)
    List<Player> findMembersByTeamUuids(@Param("uuids") List<UUID> uuids);
}
