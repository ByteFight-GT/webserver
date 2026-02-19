package org.bytefight.webserver.auth.infra;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.auth.domain.dto.AdminUserWithPlayerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
        SELECT new org.bytefight.webserver.auth.domain.dto.AdminUserWithPlayerDto(
            u.id,
            u.uuid,
            u.email,
            u.createdAt,
            u.isAdmin,
            p.id,
            p.username
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
    """)
    Page<AdminUserWithPlayerDto> findAllWithPlayers(Pageable pageable);

    @Query("""
        SELECT new org.bytefight.webserver.auth.domain.dto.AdminUserWithPlayerDto(
            u.id,
            u.uuid,
            u.email,
            u.createdAt,
            u.isAdmin,
            p.id,
            p.username
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
        WHERE u.id IN :ids
    """)
    Page<AdminUserWithPlayerDto> findAllWithPlayersByIdIn(List<Long> ids, Pageable pageable);

    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(UUID uuid);
    boolean existsByEmailIgnoreCase(String email);
}
