package org.bytefight.webserver.submission.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubmissionStatusDto {
    @NotNull long usedStorageSize;
    @NotNull long totalStorageSize;
}
