package com.example.botfightwebserver.storage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Value
@AllArgsConstructor
@Builder
public class StoredObject {
    UUID uuid;
    String filename;
    String contentType;
    Long size;
    String sha256;
    String storagePath;

    public static StoredObject from(FileRecord record) {
        return StoredObject.builder()
                .uuid(record.getUuid())
                .filename(record.getFilename())
                .contentType(record.getContentType())
                .size(record.getSize())
                .sha256(record.getSha256())
                .storagePath(record.getStoragePath())
                .build();
    }
}
