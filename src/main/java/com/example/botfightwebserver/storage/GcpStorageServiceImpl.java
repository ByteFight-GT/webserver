package com.example.botfightwebserver.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class GcpStorageServiceImpl implements StorageService {

    private final Storage storage;
    private final Clock clock;

    @Value("${GCP_BUCKET}")
    private String bucketName;

    public GcpStorageServiceImpl(Storage storage, Clock clock) {
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public String uploadFile(Long teamId, MultipartFile file) {
        try {
            String filename = generateFileName(teamId, file.getOriginalFilename());
            BlobId blobId = BlobId.of(bucketName, filename);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
            Blob blob = storage.create(blobInfo, file.getBytes());

            return filename;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Failed to upload file ", e);
        }
    }

    public void verifyAccess() {
        try {
            storage.get(bucketName);
        } catch (Exception e) {
            throw new RuntimeException("Cannot access bucket: " + bucketName + ". Error: " + e.getMessage());
        }
    }

    public String generateFileName(Long teamId, String originalFileName) {
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("TEAM_%s/%s_%s", teamId, originalFileName, timestamp);
    }
}