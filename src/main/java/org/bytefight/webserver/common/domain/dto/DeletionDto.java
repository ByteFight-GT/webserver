package org.bytefight.webserver.common.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.common.domain.AuditableSoftDeletableEntity;
import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.common.domain.SoftDeletableEntity;

import java.time.Instant;

@Value
@Builder
public class DeletionDto {
    @NotNull
    boolean isDeleted;
    Instant deletedAt;

    public static DeletionDto from(SoftDeletableEntity softDeletableEntity) {
        return DeletionDto.builder()
                .isDeleted(softDeletableEntity.isDeleted())
                .deletedAt(softDeletableEntity.getDeletedAt())
                .build();
    }

    public static DeletionDto from(AuditableSoftDeletableEntity auditableSoftDeletableEntity) {
        return DeletionDto.builder()
                .isDeleted(auditableSoftDeletableEntity.isDeleted())
                .deletedAt(auditableSoftDeletableEntity.getDeletedAt())
                .build();
    }
}
