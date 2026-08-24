package org.bytefight.webserver.gamematch.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.bytefight.webserver.gamematch.domain.dto.GameOutcomeReasonManifestEntry;
import org.bytefight.webserver.gamematch.infra.GameOutcomeReasonRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GameOutcomeReasonService {
  private static final List<GameOutcomeReasonManifestEntry> DEFAULT_REASONS =
      List.of(
          new GameOutcomeReasonManifestEntry("timeout", "Timeout"),
          new GameOutcomeReasonManifestEntry("code_error", "Code error"));

  private final GameOutcomeReasonRepository gameOutcomeReasonRepository;

  public void ensureDefaultReasons(Competition competition) {
    DEFAULT_REASONS.stream()
        .filter(
            defaultReason ->
                !gameOutcomeReasonRepository.existsByCompetitionAndCode(
                    competition, defaultReason.code()))
        .map(defaultReason -> newReason(competition, defaultReason))
        .forEach(gameOutcomeReasonRepository::save);
  }

  @Transactional
  public void registerManifest(
      Competition competition, List<GameOutcomeReasonManifestEntry> manifestReasons) {
    if (manifestReasons == null) {
      throw new IllegalArgumentException("Outcome-reason manifest is required");
    }
    Set<String> reasonCodes =
        manifestReasons.stream()
            .peek(this::validateManifestEntry)
            .map(GameOutcomeReasonManifestEntry::code)
            .collect(Collectors.toSet());
    if (reasonCodes.size() != manifestReasons.size()) {
      throw new IllegalArgumentException("Outcome-reason manifest contains duplicate codes");
    }

    manifestReasons.forEach(
        manifestReason ->
            gameOutcomeReasonRepository
                .findByCompetitionAndCode(competition, manifestReason.code())
                .orElseGet(
                    () ->
                        gameOutcomeReasonRepository.save(newReason(competition, manifestReason))));
  }

  private GameOutcomeReason newReason(
      Competition competition, GameOutcomeReasonManifestEntry manifestReason) {
    GameOutcomeReason reason = new GameOutcomeReason();
    reason.setCompetition(competition);
    reason.setCode(manifestReason.code());
    reason.setDisplayLabel(manifestReason.defaultLabel());
    reason.setVisible(true);
    return reason;
  }

  private void validateManifestEntry(GameOutcomeReasonManifestEntry manifestReason) {
    if (manifestReason == null
        || manifestReason.code() == null
        || manifestReason.code().isBlank()
        || manifestReason.code().length() > 100
        || !manifestReason.code().matches("^[a-z0-9_]+$")
        || manifestReason.defaultLabel() == null
        || manifestReason.defaultLabel().isBlank()
        || manifestReason.defaultLabel().length() > 255) {
      throw new IllegalArgumentException(
          "Outcome-reason manifest entries require a code and label");
    }
  }
}
