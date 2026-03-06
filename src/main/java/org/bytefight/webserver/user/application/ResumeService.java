package org.bytefight.webserver.user.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResumeService {
    private final LocalStorageService storageService;
    private final FileRecordRepository resumeRepository;

//    public DownloadLinkDto getDownloadLink(User requestingUser) {
//        return storageService.getDownloadLink(requestingUser.getResume().toString(), Duration.ofMinutes(5));
//    }

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
            throw new IllegalArgumentException("Please submit your resume in PDF, DOC, or DOCX format");
        }

        FileRecord resume = storageService.store(file, "resumes/" + requestingUser.getUuid(), requestingUser.getUsername() + "_resume");
        return resumeRepository.save(resume);
    }
}