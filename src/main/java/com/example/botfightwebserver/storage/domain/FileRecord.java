package com.example.botfightwebserver.storage.domain;

import com.example.botfightwebserver.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_records")
public class FileRecord extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "sha256", nullable = false)
    private String sha256;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;
}