package org.bytefight.webserver.user.infra;


import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<FileRecord, Long> {
//    Optional<GameMatchFile> findByGameMatch_UuidAndSlugAndTeamIsNull(UUID gameMatchUuid, String slug);
//    Optional<GameMatchFile> findByGameMatch_UuidAndSlugAndTeam(UUID gameMatchUuid, String slug, Team team);
}
