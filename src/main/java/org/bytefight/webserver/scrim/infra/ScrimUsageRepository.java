package org.bytefight.webserver.scrim.infra;

import java.time.Instant;
import java.util.Optional;

import org.bytefight.webserver.scrim.domain.ScrimUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrimUsageRepository
    extends JpaRepository<ScrimUsage, Long>, ScrimUsageRepositoryCustom {

  Optional<ScrimUsage> findByTeam_IdAndWindowKindAndWindowStart(
      Long teamId, String windowKind, Instant windowStart);
}
