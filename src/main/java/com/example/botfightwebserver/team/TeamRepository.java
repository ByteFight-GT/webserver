package com.example.botfightwebserver.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);
    int countByCurrentSubmissionNotNull();
    @Query("SELECT t FROM Team t WHERE t.currentSubmission IS NOT NULL")
    Page<Team> findAllTeamsWithCurrentSubmission(Pageable pageable);
}
