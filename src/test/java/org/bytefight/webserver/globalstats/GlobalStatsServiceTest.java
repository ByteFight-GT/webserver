package org.bytefight.webserver.globalstats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.globalstats.application.GlobalStatsService;
import org.bytefight.webserver.globalstats.application.StatCollector;
import org.bytefight.webserver.globalstats.infra.GlobalStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class GlobalStatsServiceTest extends FullStackIntegrationTestBase {
  @Autowired private GlobalStatRepository globalStatRepository;

  @BeforeEach
  void clearStats() {
    globalStatRepository.deleteAll();
  }

  @Test
  void refreshAllInsertsThenUpdatesStats() {
    AtomicLong matches = new AtomicLong(10);
    GlobalStatsService service =
        new GlobalStatsService(
            globalStatRepository,
            List.of(collector("total_matches", matches::get), collector("total_teams", () -> 3)));

    service.refreshAll();
    assertThat(service.getAllStats())
        .containsExactlyInAnyOrderEntriesOf(Map.of("total_matches", 10L, "total_teams", 3L));

    matches.set(25);
    service.refreshAll();
    assertThat(service.getAllStats()).containsEntry("total_matches", 25L).hasSize(2);
  }

  @Test
  void failingCollectorDoesNotBlockOthers() {
    GlobalStatsService service =
        new GlobalStatsService(
            globalStatRepository,
            List.of(
                collector(
                    "broken",
                    () -> {
                      throw new IllegalStateException("boom");
                    }),
                collector("total_teams", () -> 7)));

    service.refreshAll();

    assertThat(service.getAllStats())
        .containsOnlyKeys("total_teams")
        .containsEntry("total_teams", 7L);
  }

  @Test
  void rejectsInvalidOrDuplicateSlugs() {
    assertThatThrownBy(
            () ->
                new GlobalStatsService(
                    globalStatRepository, List.of(collector("Total-Matches", () -> 0))))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(
            () ->
                new GlobalStatsService(
                    globalStatRepository,
                    List.of(collector("total_teams", () -> 0), collector("total_teams", () -> 1))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void serviceBeanStartsWithNoCollectorsRegistered(@Autowired GlobalStatsService service) {
    service.refreshAll();

    assertThat(service.getAllStats()).isEmpty();
  }

  private static StatCollector collector(String metric, LongSupplier value) {
    return new StatCollector() {
      @Override
      public String metric() {
        return metric;
      }

      @Override
      public long collect() {
        return value.getAsLong();
      }
    };
  }
}
