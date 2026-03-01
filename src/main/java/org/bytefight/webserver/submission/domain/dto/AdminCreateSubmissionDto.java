package org.bytefight.webserver.submission.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.springframework.web.multipart.MultipartFile;

@Value
public class AdminCreateSubmissionDto {
  @NotNull Long teamId;
  String description;
  SubmissionValidity validity;
  @NotNull MultipartFile file;
}
