package com.example.botfightwebserver.storage.infra;

import com.example.botfightwebserver.storage.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByUuid(UUID uuid);
}
