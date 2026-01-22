package com.example.botfightwebserver.competition.infra;

import com.example.botfightwebserver.competition.domain.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findBySlug(String slug);
}
