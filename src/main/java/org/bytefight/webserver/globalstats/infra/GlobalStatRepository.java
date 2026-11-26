package org.bytefight.webserver.globalstats.infra;

import java.util.Optional;

import org.bytefight.webserver.globalstats.domain.GlobalStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalStatRepository extends JpaRepository<GlobalStat, Long> {
  Optional<GlobalStat> findByMetric(String metric);
}
