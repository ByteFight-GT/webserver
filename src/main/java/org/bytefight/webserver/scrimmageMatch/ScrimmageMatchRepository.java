package org.bytefight.webserver.scrimmageMatch;

import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrimmageMatchRepository extends JpaRepository<ScrimmageMatch, Long> {

    @Query("SELECT COUNT(s) FROM ScrimmageMatch s JOIN s.match m WHERE m.status = :status AND s.initiatorTeam = :team")
    Long countByMatchStatusAndInitiatorTeam(
        @Param("status") MatchStatus status,
        @Param("team") Team team
    );
}
