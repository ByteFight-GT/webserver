package com.example.botfightwebserver.submission;

import lombok.Value;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Value
public class UploadSubmissionDto {
    MultipartFile file;
    Boolean isAutoSet;
}
