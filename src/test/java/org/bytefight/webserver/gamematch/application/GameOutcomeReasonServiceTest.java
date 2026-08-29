package org.bytefight.webserver.gamematch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameOutcomeReasonServiceTest {
  @Mock private GameOutcomeReasonRepository gameOutcomeReasonRepository;

  @InjectMocks private GameOutcomeReasonService gameOutcomeReasonService;

  @Test
  void ensureDefaultReasonsAddsTheUniversalReasonCodes() {
    Competition competition = new Competition();
    when(gameOutcomeReasonRepository.existsByCompetitionAndCode(eq(competition), any()))
        .thenReturn(false);

    gameOutcomeReasonService.ensureDefaultReasons(competition);

    ArgumentCaptor<GameOutcomeReason> reasonCaptor =
        ArgumentCaptor.forClass(GameOutcomeReason.class);
    verify(gameOutcomeReasonRepository, times(2)).save(reasonCaptor.capture());
    assertThat(reasonCaptor.getAllValues())
        .extracting(GameOutcomeReason::getCode, GameOutcomeReason::getDisplayLabel)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple("timeout", "Timeout"),
            org.assertj.core.groups.Tuple.tuple("code_error", "Code error"));
  }

  @Test
  void createReasonAddsAnAdminDefinedCodeToTheCompetitionCatalog() {
    Competition competition = new Competition();
    when(gameOutcomeReasonRepository.existsByCompetitionAndCode(competition, "capture_flag"))
        .thenReturn(false);

    GameOutcomeReason createdReason =
        gameOutcomeReasonService.createReason(competition, "capture_flag", "Captured flag");

    verify(gameOutcomeReasonRepository).save(createdReason);
    assertThat(createdReason.getCompetition()).isSameAs(competition);
    assertThat(createdReason.getCode()).isEqualTo("capture_flag");
    assertThat(createdReason.getDisplayLabel()).isEqualTo("Captured flag");
    assertThat(createdReason.isVisible()).isTrue();
  }
}
