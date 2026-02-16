package org.bytefight.webserver.submission.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.web.multipart.MultipartFile;
import org.bytefight.webserver.submission.domain.SubmissionValidity;

@Value
public class AdminCreateSubmissionDto {
    @NotNull Long teamId;
    String description;
    SubmissionValidity validity;
    @NotNull MultipartFile file;
}
