package org.bytefight.webserver.auth.infra;

import org.bytefight.webserver.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(UUID uuid);
    boolean existsByEmailIgnoreCase(String email);
}
