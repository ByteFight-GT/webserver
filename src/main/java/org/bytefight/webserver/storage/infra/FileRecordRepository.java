package org.bytefight.webserver.storage.infra;

import org.bytefight.webserver.storage.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByUuid(UUID uuid);
    Optional<FileRecord> findByUuidAndIsDeletedFalse(UUID uuid);
}
