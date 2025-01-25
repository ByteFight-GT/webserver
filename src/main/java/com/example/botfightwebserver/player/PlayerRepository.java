package com.example.botfightwebserver.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByEmail(String email);
    List<Player> findByTeamId(Long teamId);
    Optional<Player> findByAuthId(UUID authId);
    boolean existsByAuthId(UUID authId);
}
