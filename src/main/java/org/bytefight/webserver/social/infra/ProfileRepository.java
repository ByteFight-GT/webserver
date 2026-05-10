package org.bytefight.webserver.social.infra;

import org.bytefight.webserver.player.domain.Player;
import org.springframework.data.domain.Page;
import org.bytefight.webserver.social.domain.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long>, JpaSpecificationExecutor<Profile> {
    Optional<Profile> findByPlayerAndIsDeletedFalse(Player player);
    Boolean existsByPlayerAndIsDeletedFalse(Player player);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Profile p WHERE p.player.username = :username")
    Boolean existsByPlayerUsername(@Param("username") String username);
    Page<Profile> findByPlayerUsername(@Param("username") String username, Pageable pageable);

    @Query("SELECT p FROM Profile p WHERE p.description LIKE %:keyword%")
    Page<Profile> findByDescriptionKeyword(@Param("keyword") String keyword);

    Page<Profile> findByMajor(@Param("major") String major);

    Page<Profile> findByYear(@Param("year") Integer year);
}
