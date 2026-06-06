package org.bytefight.webserver.social.infra;

import java.util.Optional;

import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.social.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProfileRepository extends JpaRepository<Profile, Long>, JpaSpecificationExecutor<Profile> {
    Optional<Profile> findByPlayerAndIsDeletedFalse(Player player);

    boolean existsByPlayerAndIsDeletedFalse(Player player);
}