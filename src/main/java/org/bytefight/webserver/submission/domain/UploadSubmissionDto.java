package org.bytefight.webserver.submission.domain;

import lombok.Value;
import org.springframework.web.multipart.MultipartFile;

@Value
public class UploadSubmissionDto {
    MultipartFile file;
    String description;
    Boolean isAutoSet;
}
