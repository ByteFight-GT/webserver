package com.example.botfightwebserver.whitelist.infra;

import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.whitelist.domain.WhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhitelistEntryRepository extends JpaRepository<WhitelistEntry, Long> {
    boolean existsByCompetitionAndEmail(Competition competition, String email);
}
