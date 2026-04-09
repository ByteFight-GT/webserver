package org.bytefight.webserver.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.bytefight.webserver.user.domain.User;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id", updatable = false)
  private User createdByUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_user_id")
  private User updatedByUser;
}
