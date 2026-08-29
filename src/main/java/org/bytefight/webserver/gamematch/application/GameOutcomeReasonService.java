package org.bytefight.webserver.gamematch.application;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class GameOutcomeReasonService {
  private static final List<DefaultReason> DEFAULT_REASONS =
      List.of(
          new DefaultReason("timeout", "Timeout"),
          new DefaultReason("code_error", "Code error"));

  private final GameOutcomeReasonRepository gameOutcomeReasonRepository;

  public void ensureDefaultReasons(Competition competition) {
    DEFAULT_REASONS.stream()
        .filter(
            defaultReason ->
                !gameOutcomeReasonRepository.existsByCompetitionAndCode(
                    competition, defaultReason.code()))
        .map(
            defaultReason ->
                newReason(competition, defaultReason.code(), defaultReason.label()))
        .forEach(gameOutcomeReasonRepository::save);
  }

  public GameOutcomeReason createReason(Competition competition, String code, String displayLabel) {
    if (gameOutcomeReasonRepository.existsByCompetitionAndCode(competition, code)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Outcome reason code already exists for this competition");
    }
    return gameOutcomeReasonRepository.save(newReason(competition, code, displayLabel));
  }

  private GameOutcomeReason newReason(Competition competition, String code, String displayLabel) {
    GameOutcomeReason reason = new GameOutcomeReason();
    reason.setCompetition(competition);
    reason.setCode(code);
    reason.setDisplayLabel(displayLabel);
    reason.setVisible(true);
    return reason;
  }

  private record DefaultReason(String code, String label) {}
}
