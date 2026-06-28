package org.bytefight.webserver.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {
  @Column(name = "deleted_at")
  private Instant deletedAt;

  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
