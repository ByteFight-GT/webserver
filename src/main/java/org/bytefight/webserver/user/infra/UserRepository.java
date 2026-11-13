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
            r.uuid
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
        LEFT JOIN u.resume r
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
            r.uuid
        )
        FROM User u
        LEFT JOIN Player p ON p.user = u
        LEFT JOIN u.resume r
        WHERE u.id IN :ids
    """)
  Page<AdminUserWithPlayerAndResumeDto> findAllWithPlayersByIdIn(List<Long> ids, Pageable pageable);

  /**
   * Case-insensitive filter. {@code pattern} is matched against email or player username, and
   * {@code emailPattern} against email only. Both must be lowercase LIKE patterns using {@code !}
   * as the escape character; pass {@code "%"} to disable either one.
   */
  @Query(
      value =
          """
            SELECT new org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto(
                u.id,
                u.uuid,
                u.email,
                u.createdAt,
                u.isAdmin,
                p.id,
                p.username,
                r.uuid
            )
            FROM User u
            LEFT JOIN Player p ON p.user = u
            LEFT JOIN u.resume r
            WHERE (LOWER(u.email) LIKE :pattern ESCAPE '!'
                OR LOWER(p.username) LIKE :pattern ESCAPE '!')
              AND LOWER(u.email) LIKE :emailPattern ESCAPE '!'
          """,
      countQuery =
          """
            SELECT COUNT(u)
            FROM User u
            LEFT JOIN Player p ON p.user = u
            WHERE (LOWER(u.email) LIKE :pattern ESCAPE '!'
                OR LOWER(p.username) LIKE :pattern ESCAPE '!')
              AND LOWER(u.email) LIKE :emailPattern ESCAPE '!'
          """)
  Page<AdminUserWithPlayerAndResumeDto> searchWithPlayers(
      String pattern, String emailPattern, Pageable pageable);

  /** Distinct lowercase email domains with user counts, most users first. Rows are [domain, count]. */
  @Query(
      value =
          """
            SELECT LOWER(SPLIT_PART(email, '@', 2)) AS domain, COUNT(*) AS user_count
            FROM users
            WHERE POSITION('@' IN email) > 0
            GROUP BY LOWER(SPLIT_PART(email, '@', 2))
            ORDER BY user_count DESC, domain
          """,
      nativeQuery = true)
  List<Object[]> countUsersByEmailDomain();

  Optional<User> findByEmail(String email);

  Optional<User> findByUuid(UUID uuid);

  boolean existsByEmailIgnoreCase(String email);
}
