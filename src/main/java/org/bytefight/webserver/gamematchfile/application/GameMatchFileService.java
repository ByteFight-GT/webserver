package org.bytefight.webserver.gamematchfile.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.bytefight.webserver.gamematchfile.infra.GameMatchFileRepository;
import org.bytefight.webserver.player.application.PlayerService;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameMatchFileService {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final GameMatchFileRepository gameMatchFileRepository;
    private final LocalStorageService storageService;

    public Optional<GameMatchFile> getByGameMatchAndSlugNoTeam(UUID gameMatchId, String slug) {
        return gameMatchFileRepository.findByGameMatch_UuidAndSlugAndTeamIsNull(gameMatchId, slug);
    }

    public Optional<GameMatchFile> getByGameMatchAndSlugAndTeam(UUID gameMatchId, String slug, Team team) {
        return gameMatchFileRepository.findByGameMatch_UuidAndSlugAndTeam(gameMatchId, slug, team);
    }

    public DownloadLinkDto getPublicDownloadLink(GameMatchFile gameMatchFile) {
        if (gameMatchFile.getVisibility() == GameMatchFileVisibility.everyone) {
            return storageService.getDownloadLink(gameMatchFile.getFileRecord().getUuid().toString(), Duration.ofMinutes(5));
        }

        throw new AccessDeniedException("You do not have permission to access this file");
    }

    public DownloadLinkDto getDownloadLink(User requestingUser, GameMatchFile gameMatchFile) {
        if (requestingUser.isAdmin()) {
            return storageService.getDownloadLink(gameMatchFile.getFileRecord().getUuid().toString(), Duration.ofMinutes(5));
        }

        if (gameMatchFile.getVisibility() == GameMatchFileVisibility.admin) {
            throw new AccessDeniedException("You do not have permission to access this file");
        }

        if (gameMatchFile.getVisibility() == GameMatchFileVisibility.team && gameMatchFile.getTeam() != null) {
            Player player = playerService.getPlayer(requestingUser).orElseThrow(() -> new AccessDeniedException("You do not have permission to access this file"));
            if (teamService.isMember(gameMatchFile.getTeam(), player)) {
                return storageService.getDownloadLink(gameMatchFile.getFileRecord().getUuid().toString(), Duration.ofMinutes(5));
            }
        }

        if (gameMatchFile.getVisibility() == GameMatchFileVisibility.everyone) {
            return storageService.getDownloadLink(gameMatchFile.getFileRecord().getUuid().toString(), Duration.ofMinutes(5));
        }

        throw new AccessDeniedException("You do not have permission to access this file");
    }

    @Transactional
    public GameMatchFile uploadGameMatchFile(MultipartFile file, String slug, GameMatch gameMatch, GameMatchFileVisibility visibility, Team team) throws IOException {
        if (visibility == GameMatchFileVisibility.team && team == null) {
            throw new IllegalArgumentException("You must specify a team when using visibility 'team'");
        }

        FileRecord storedFile = storageService.store(file, "game_match_files/" + gameMatch.getUuid() + "/", file.getOriginalFilename());

        GameMatchFile gameMatchFile = new GameMatchFile();
        gameMatchFile.setUuid(storedFile.getUuid());
        gameMatchFile.setGameMatch(gameMatch);
        gameMatchFile.setSlug(slug);
        gameMatchFile.setFileRecord(storedFile);
        gameMatchFile.setTeam(team);
        gameMatchFile.setVisibility(visibility);

        return gameMatchFileRepository.save(gameMatchFile);
    }
}
