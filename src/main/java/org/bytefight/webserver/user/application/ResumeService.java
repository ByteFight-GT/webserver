package org.bytefight.webserver.user.application;

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
import org.bytefight.webserver.user.infra.ResumeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.File;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {
    private final LocalStorageService storageService;
    private final ResumeRepository resumeRepository;

    public Optional<FileRecord> getResume(long id) {
        return resumeRepository.findById(id);
    }

    public DownloadLinkDto getDownloadLink(User requestingUser) {
        return storageService.getDownloadLink(requestingUser.getResume().toString(), Duration.ofMinutes(5));
    }

    @Transactional
    public FileRecord uploadResume(MultipartFile file, User requestingUser) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("No resume is uploaded");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IOException("File name is null");
        }

        String lower = fileName.toLowerCase();

        Set<String> allowed_type = Set.of(".pdf", ".doc", ".docx");

        boolean valid = allowed_type.stream().anyMatch(lower::endsWith);

        if (!valid) {
            throw new IOException("Please submit your resume in PDF, DOC, or DOCX format");
        }

        FileRecord resume = storageService.store(file, "resumes/" + requestingUser.getUuid(), file.getOriginalFilename());
        return resumeRepository.save(resume);
    }
}
