package com.example.botfightwebserver.submission.domain;

import lombok.Value;
import org.springframework.web.multipart.MultipartFile;

@Value
public class UploadSubmissionDto {
    MultipartFile file;
    Boolean isAutoSet;
}
