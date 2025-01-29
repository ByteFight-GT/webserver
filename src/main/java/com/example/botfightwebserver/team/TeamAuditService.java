package com.example.botfightwebserver.team;

import com.example.botfightwebserver.glicko.GlickoHistoryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.Area;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TeamAuditService {

    private final Clock clock;

    @PersistenceContext
    private EntityManager em;

    public List<GlickoHistoryDTO> getGlickoHistory(Long teamId) {
        AuditReader auditReader = AuditReaderFactory.get(em);

        List<Number> revisions = auditReader.getRevisions(Team.class, teamId);
        AtomicInteger counter = new AtomicInteger(0);
        return revisions.stream()
            .filter(rev -> counter.getAndIncrement() % 4 == 0)
            .map(rev -> {
                Team team = auditReader.find(Team.class, teamId, rev);
                return GlickoHistoryDTO.builder()
                    .teamId(teamId)
                    .glicko(team.getGlicko())
                    .revisionDate(LocalDateTime.ofInstant(
                        auditReader.getRevisionDate(rev).toInstant(),
                        clock.getZone()))
                    .build();
            })
            .collect(Collectors.toList());
    }
}
