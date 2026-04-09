package org.bytefight.webserver.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.user.domain.User;

@Value
@Builder
public class ResumeDto {
  @NotNull String uuid;
  @NotNull String fileRecordUuid;
  DownloadLinkDto downloadLink;

  public static ResumeDto from(DownloadLinkDto downloadLink, User user) {
    return ResumeDto.builder()
        .uuid(user.getUuid().toString())
        .fileRecordUuid(user.getResume().toString())
        .downloadLink(downloadLink)
        .build();
  }
}
