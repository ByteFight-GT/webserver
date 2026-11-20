package org.bytefight.webserver.profile.infra;

import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.profile.domain.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
  @EntityGraph(attributePaths = {"player", "player.user"})
  Optional<Profile> findByPlayerId(Long playerId);

  @EntityGraph(attributePaths = {"player", "player.user"})
  @Query(
      value =
          """
        SELECT profile
        FROM Profile profile
        JOIN profile.player player
        WHERE (:query IS NULL OR :query = ''
          OR LOWER(player.username) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(player.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.description) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.major) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.school) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:major IS NULL OR LOWER(profile.major) LIKE LOWER(CONCAT('%', :major, '%')))
          AND (:school IS NULL OR LOWER(profile.school) LIKE LOWER(CONCAT('%', :school, '%')))
          AND (:graduationYear IS NULL OR profile.graduationYear = :graduationYear)
        ORDER BY LOWER(player.username), player.id
        """,
      countQuery =
          """
        SELECT COUNT(profile)
        FROM Profile profile
        JOIN profile.player player
        WHERE (:query IS NULL OR :query = ''
          OR LOWER(player.username) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(player.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.description) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.major) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(profile.school) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:major IS NULL OR LOWER(profile.major) LIKE LOWER(CONCAT('%', :major, '%')))
          AND (:school IS NULL OR LOWER(profile.school) LIKE LOWER(CONCAT('%', :school, '%')))
          AND (:graduationYear IS NULL OR profile.graduationYear = :graduationYear)
        """)
  Page<Profile> searchProfiles(
      @Param("query") String query,
      @Param("major") String major,
      @Param("school") String school,
      @Param("graduationYear") Integer graduationYear,
      Pageable pageable);

  @EntityGraph(attributePaths = {"player", "player.user"})
  @Query(
      value =
          """
        SELECT profile
        FROM Profile profile
        JOIN profile.player candidate
        WHERE candidate.id <> :playerId
        ORDER BY
          (CASE WHEN :major IS NOT NULL AND profile.major IS NOT NULL
                AND LOWER(profile.major) = LOWER(:major) THEN 1 ELSE 0 END
           + CASE WHEN :school IS NOT NULL AND profile.school IS NOT NULL
                AND LOWER(profile.school) = LOWER(:school) THEN 1 ELSE 0 END
           + CASE WHEN :graduationYear IS NOT NULL
                AND profile.graduationYear = :graduationYear THEN 1 ELSE 0 END) DESC,
          LOWER(candidate.username), candidate.id
        """,
      countQuery =
          """
        SELECT COUNT(profile)
        FROM Profile profile
        WHERE profile.player.id <> :playerId
        """)
  Page<Profile> findSuggestions(
      @Param("playerId") Long playerId,
      @Param("major") String major,
      @Param("school") String school,
      @Param("graduationYear") Integer graduationYear,
      Pageable pageable);

  @EntityGraph(attributePaths = {"player", "player.user"})
  @Query("SELECT profile FROM Profile profile WHERE profile.player.user.uuid = :playerUuid")
  Optional<Profile> findByPlayerUuid(@Param("playerUuid") UUID playerUuid);
}
