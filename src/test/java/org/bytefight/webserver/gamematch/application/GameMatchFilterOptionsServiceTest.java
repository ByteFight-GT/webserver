package org.bytefight.webserver.gamematch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchFilterOptionsDto;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameMatchFilterOptionsServiceTest {
  @Mock private GameMatchRepository gameMatchRepository;
  @Mock private GameOutcomeReasonRepository gameOutcomeReasonRepository;

  @InjectMocks private GameMatchFilterOptionsService gameMatchFilterOptionsService;

  @Test
  void getFilterOptionsReturnsObservedMapsVisibleReasonsAndOtherAvailability() {
    Competition competition = new Competition();
    GameOutcomeReason timeout = new GameOutcomeReason();
    timeout.setCode("timeout");
    timeout.setDisplayLabel("Timeout");
    timeout.setVisible(true);

    when(gameMatchRepository.findDistinctMapCodesByCompetition(competition))
        .thenReturn(List.of("arena_01", "arena_02"));
    when(gameOutcomeReasonRepository.findByCompetitionAndVisibleTrueOrderByDisplayLabelAsc(
            competition))
        .thenReturn(List.of(timeout));
    when(gameMatchRepository.existsUnregisteredOutcomeReasonByCompetition(competition))
        .thenReturn(true);

    GameMatchFilterOptionsDto options = gameMatchFilterOptionsService.getFilterOptions(competition);

    assertThat(options.getMapCodes()).containsExactly("arena_01", "arena_02");
    assertThat(options.getOutcomeReasons())
        .extracting(reason -> reason.getCode(), reason -> reason.getDisplayLabel())
        .containsExactly(org.assertj.core.groups.Tuple.tuple("timeout", "Timeout"));
    assertThat(options.isHasOtherOutcomeReasons()).isTrue();
  }
}
