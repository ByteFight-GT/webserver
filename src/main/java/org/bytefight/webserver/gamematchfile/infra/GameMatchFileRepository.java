package org.bytefight.webserver.gamematchfile.infra;

import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMatchFileRepository extends JpaRepository<GameMatchFile, Long> {
  Optional<GameMatchFile> findByGameMatch_UuidAndSlugAndTeamIsNull(UUID gameMatchUuid, String slug);

  Optional<GameMatchFile> findByGameMatch_UuidAndSlugAndTeam(
      UUID gameMatchUuid, String slug, Team team);
}
