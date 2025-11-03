package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);
    int countByCurrentSubmissionNotNull();
    @Query("SELECT t FROM Team t ORDER BY CASE WHEN t.currentSubmission IS NULL THEN 1 ELSE 0 END, t.glicko DESC")
    Page<Team> findTeamsPaginated(Pageable pageable);
    Optional<Team> findByTeamCode(String teamCode);
    Optional<Team> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);

    @Query(value = """
        SELECT 1+COUNT(t2.id)
        FROM Team t
        LEFT JOIN Team t2
            ON (t2.glicko, t2.matchesPlayed, t2.id) > (t.glicko, t.matchesPlayed, t.id)
        WHERE t.uuid = :uuid
        GROUP BY t.id
    """)
    int findRankByUuid(UUID uuid);

    List<Team> findAllByOrderByGlickoDescMatchesPlayedAscIdAsc();
}
