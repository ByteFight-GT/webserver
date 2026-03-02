package org.bytefight.webserver.common.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import org.bytefight.webserver.common.domain.AuditableSoftDeletableEntity;
import org.bytefight.webserver.common.domain.SoftDeletableEntity;

@Value
@Builder
public class DeletionDto {
  @NotNull boolean isDeleted;
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
