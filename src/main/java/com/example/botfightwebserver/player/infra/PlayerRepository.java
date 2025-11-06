package com.example.botfightwebserver.player.infra;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByUserEmail(String email);
    List<Player> findByTeamId(Long teamId);
    Optional<Player> findByUserUuid(UUID authId);
    Optional<Player> findByUser(User user);
    boolean existsByUserUuid(UUID authId);
    boolean existsByName(String name);

    @Query("""
        SELECT p
        FROM Player p
        WHERE p.team.uuid IN :uuids
    """)
    List<Player> findMembersByTeamUuids(@Param("uuids") List<UUID> uuids);

    List<UUID> team(Team team);
}
