package org.bytefight.webserver.ladder;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytefight.webserver.ladder.domain.DefaultLadderSettings;
import org.bytefight.webserver.ladder.domain.Ladder;
import org.junit.jupiter.api.Test;

/**
 * Guards the default for {@code Ladder.maxQueuedPerTeam} (webserver#159).
 *
 * <p>The field is a primitive, so without an initializer Hibernate writes an explicit 0 on INSERT —
 * bypassing the column's {@code DEFAULT 10} — and the ladder then rejects every user-created match
 * with a 429. {@code @Builder.Default} is load-bearing here: Lombok silently ignores the
 * initializer on the builder path without it, which would leave {@link DefaultLadderSettings} back
 * at 0.
 *
 * <p>Deliberately a plain unit test: asserting a field initializer needs no database or container.
 */
class LadderDefaultsTest {

  @Test
  void defaultMatchesTheColumnDefault() {
    assertThat(Ladder.DEFAULT_MAX_QUEUED_PER_TEAM).isEqualTo(10);
  }

  @Test
  void noArgsConstructorAppliesTheDefault() {
    assertThat(new Ladder().getMaxQueuedPerTeam()).isEqualTo(Ladder.DEFAULT_MAX_QUEUED_PER_TEAM);
  }

  @Test
  void builderAppliesTheDefault() {
    assertThat(Ladder.builder().build().getMaxQueuedPerTeam())
        .isEqualTo(Ladder.DEFAULT_MAX_QUEUED_PER_TEAM);
  }

  @Test
  void defaultLadderSettingsAppliesTheDefault() {
    assertThat(DefaultLadderSettings.baseline1500NoInflation().getMaxQueuedPerTeam())
        .isEqualTo(Ladder.DEFAULT_MAX_QUEUED_PER_TEAM);
  }
}
