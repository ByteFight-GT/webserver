package org.bytefight.webserver.gamematch.infra;

import java.util.List;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.GameOutcomeReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameOutcomeReasonRepository extends JpaRepository<GameOutcomeReason, Long> {
  boolean existsByCompetitionAndCode(Competition competition, String code);

  List<GameOutcomeReason> findByCompetitionAndVisibleTrueOrderByDisplayLabelAsc(
      Competition competition);

  List<GameOutcomeReason> findByCompetitionIdOrderByCodeAsc(Long competitionId);
}
