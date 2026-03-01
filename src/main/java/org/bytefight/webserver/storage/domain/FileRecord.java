package org.bytefight.webserver.storage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import org.bytefight.webserver.common.domain.SoftDeletableEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_records")
public class FileRecord extends SoftDeletableEntity {
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
