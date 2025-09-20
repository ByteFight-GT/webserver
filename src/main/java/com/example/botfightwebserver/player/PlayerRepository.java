package com.example.botfightwebserver.player;

import com.example.botfightwebserver.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByUserEmail(String email);
    List<Player> findByTeamId(Long teamId);
    Optional<Player> findByUserUuid(UUID authId);
    Optional<Player> findByUser(User user);
    boolean existsByUserUuid(UUID authId);
    boolean existsByName(String name);
}
