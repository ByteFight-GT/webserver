package org.bytefight.webserver.common.domain.dto;

import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.common.domain.AuditableSoftDeletableEntity;
import org.bytefight.webserver.common.domain.SoftDeletableEntity;

import java.time.Instant;

@Value
@Builder
public class DeletionDto {
    Boolean deleted;
    Instant deletedAt;

    public static DeletionDto from(SoftDeletableEntity softDeletableEntity) {
        return DeletionDto.builder()
                .deleted(softDeletableEntity.isDeleted())
                .deletedAt(softDeletableEntity.getDeletedAt())
                .build();
    }

    public static DeletionDto from(AuditableSoftDeletableEntity auditableSoftDeletableEntity) {
        return DeletionDto.builder()
                .deleted(auditableSoftDeletableEntity.isDeleted())
                .deletedAt(auditableSoftDeletableEntity.getDeletedAt())
                .build();
    }
}