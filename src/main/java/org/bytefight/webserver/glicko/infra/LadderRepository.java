package org.bytefight.webserver.glicko.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.glicko.domain.Ladder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LadderRepository extends JpaRepository<Ladder, Long> {
    Optional<Ladder> findByCompetitionAndLadder(Competition competition, String ladder);

    Page<Ladder> findByCompetitionId(Long competitionId, Pageable pageable);
}
