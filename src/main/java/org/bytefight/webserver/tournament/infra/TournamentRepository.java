package org.bytefight.webserver.tournament.infra;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.tournament.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for tournament metadata (tournament table). */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
  Optional<Tournament> findByUuid(UUID uuid);

  Optional<Tournament> findByUuidAndCompetition(UUID uuid, Competition competition);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM Tournament t WHERE t.id = :id")
  Optional<Tournament> findByIdForUpdate(@Param("id") Long id);

  List<Tournament> findByCompetitionOrderByIdDesc(Competition competition);
}
