package org.bytefight.webserver.scrim.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

public class ScrimUsageRepositoryImpl implements ScrimUsageRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public OptionalInt tryIncrement(long teamId, String windowKind, Instant windowStart, int cap) {
    List<?> result =
        entityManager
            .createNativeQuery(
                """
                INSERT INTO scrim_usage (team_id, window_kind, window_start, count)
                VALUES (:teamId, :windowKind, :windowStart, 1)
                ON CONFLICT (team_id, window_kind, window_start)
                DO UPDATE SET count = scrim_usage.count + 1
                WHERE scrim_usage.count < :cap
                RETURNING count
                """)
            .setParameter("teamId", teamId)
            .setParameter("windowKind", windowKind)
            .setParameter("windowStart", Timestamp.from(windowStart))
            .setParameter("cap", cap)
            .getResultList();

    if (result.isEmpty()) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(((Number) result.get(0)).intValue());
  }
}
