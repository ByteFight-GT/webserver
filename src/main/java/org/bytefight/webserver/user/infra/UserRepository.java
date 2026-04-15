package org.bytefight.webserver.user.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  @Query(
      """
        SELECT new org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto(
            u.id,
            u.uuid,
            u.email,
            u.createdAt,
            u.isAdmin,
            p.id,
            p.username,
            u.resume.uuid
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
    """)
  Page<AdminUserWithPlayerAndResumeDto> findAllWithPlayers(Pageable pageable);

  @Query(
      """
        SELECT new org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto(
            u.id,
            u.uuid,
            u.email,
            u.createdAt,
            u.isAdmin,
            p.id,
            p.username,
            u.resume.uuid
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
        WHERE u.id IN :ids
    """)
  Page<AdminUserWithPlayerAndResumeDto> findAllWithPlayersByIdIn(List<Long> ids, Pageable pageable);

  Optional<User> findByEmail(String email);

  Optional<User> findByUuid(UUID uuid);

  boolean existsByEmailIgnoreCase(String email);
}
