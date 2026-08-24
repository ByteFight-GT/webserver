package org.bytefight.webserver.gamematch.application;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchFilterOptionsDto;
import org.bytefight.webserver.gamematch.domain.dto.GameOutcomeReasonDto;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GameMatchFilterOptionsService {
  private final GameMatchRepository gameMatchRepository;
  private final GameOutcomeReasonRepository gameOutcomeReasonRepository;

  public GameMatchFilterOptionsDto getFilterOptions(Competition competition) {
    List<String> mapCodes = gameMatchRepository.findDistinctMapCodesByCompetition(competition);
    List<GameOutcomeReasonDto> outcomeReasons =
        gameOutcomeReasonRepository
            .findByCompetitionAndVisibleTrueOrderByDisplayLabelAsc(competition)
            .stream()
            .map(GameOutcomeReasonDto::from)
            .toList();
    boolean hasOtherOutcomeReasons =
        gameMatchRepository.existsUnregisteredOutcomeReasonByCompetition(competition);

    return new GameMatchFilterOptionsDto(mapCodes, outcomeReasons, hasOtherOutcomeReasons);
  }
}
