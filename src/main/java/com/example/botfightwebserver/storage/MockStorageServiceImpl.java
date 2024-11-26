package com.example.botfightwebserver.storage;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MockStorageServiceImpl implements  StorageService {

    private Clock clock;

    public String uploadFile(Long teamId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }
        String fileName = file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty() ?
            file.getOriginalFilename() : "unknown";
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("TEAM_%s/%s_%s",teamId, fileName, timestamp);
    }

    @Override
    public void verifyAccess() {
        return;
    }
}
