package org.bytefight.webserver.gamematch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

  @Test
  void createReasonRejectsADuplicateCodeWithinTheCompetition() {
    Competition competition = new Competition();
    when(gameOutcomeReasonRepository.existsByCompetitionAndCode(competition, "capture_flag"))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                gameOutcomeReasonService.createReason(
                    competition, "capture_flag", "Captured flag"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void updateReasonConfigurationChangesOnlyTheAdminManagedFields() {
    GameOutcomeReason reason = new GameOutcomeReason();
    reason.setCode("capture_flag");
    reason.setDisplayLabel("Captured flag");
    reason.setVisible(true);
    when(gameOutcomeReasonRepository.findById(42L)).thenReturn(Optional.of(reason));
    when(gameOutcomeReasonRepository.save(reason)).thenReturn(reason);

    GameOutcomeReason updated =
        gameOutcomeReasonService.updateReasonConfiguration(42L, "Flag captured", false);

    assertThat(updated.getCode()).isEqualTo("capture_flag");
    assertThat(updated.getDisplayLabel()).isEqualTo("Flag captured");
    assertThat(updated.isVisible()).isFalse();
    verify(gameOutcomeReasonRepository).save(reason);
  }
}
