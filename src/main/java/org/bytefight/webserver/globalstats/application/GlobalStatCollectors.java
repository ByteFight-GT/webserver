package org.bytefight.webserver.globalstats.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.springframework.stereotype.Component;

@Component
class TotalUsersCollector implements StatCollector {
  @PersistenceContext private EntityManager entityManager;

  @Override
  public String metric() {
    return "total_users";
  }

  @Override
  public long collect() {
    return entityManager.createQuery("select count(u) from User u", Long.class).getSingleResult();
  }
}

@Component
class TotalTeamsCollector implements StatCollector {
  @PersistenceContext private EntityManager entityManager;

  @Override
  public String metric() {
    return "total_teams";
  }

  /** Soft-deleted teams are excluded. */
  @Override
  public long collect() {
    return entityManager
        .createQuery("select count(t) from Team t where t.deletedAt is null", Long.class)
        .getSingleResult();
  }
}

@Component
class TotalMatchesPlayedCollector implements StatCollector {
  @PersistenceContext private EntityManager entityManager;

  @Override
  public String metric() {
    return "total_matches_played";
  }

  /**
   * Counts matches that finished with a result; unfinished, failed and validation-only matches are
   * excluded.
   */
  @Override
  public long collect() {
    return entityManager
        .createQuery("select count(m) from GameMatch m where m.status in :statuses", Long.class)
        .setParameter(
            "statuses", List.of(MatchStatus.team_a_win, MatchStatus.team_b_win, MatchStatus.draw))
        .getSingleResult();
  }
}

@Component
class StudentCodeBytesUploadedCollector implements StatCollector {
  @PersistenceContext private EntityManager entityManager;

  @Override
  public String metric() {
    return "student_code_bytes_uploaded";
  }

  /**
   * Sums the size of every submission's file. Soft-deleted submissions are intentionally included,
   * since the platform still processed and played matches with them.
   */
  @Override
  public long collect() {
    Long total =
        entityManager
            .createQuery("select sum(s.fileRecord.size) from Submission s", Long.class)
            .getSingleResult();
    return total == null ? 0L : total;
  }
}
