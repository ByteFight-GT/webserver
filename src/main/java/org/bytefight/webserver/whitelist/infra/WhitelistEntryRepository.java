package org.bytefight.webserver.whitelist.infra;

import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.whitelist.domain.WhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhitelistEntryRepository extends JpaRepository<WhitelistEntry, Long> {
    boolean existsByCompetitionAndEmail(Competition competition, String email);
}
