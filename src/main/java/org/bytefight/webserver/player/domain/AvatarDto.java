package org.bytefight.webserver.player.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.storage.domain.DownloadLinkDto;

@Value
@Builder
public class AvatarDto {
  @NotNull String uuid;
  @NotNull String fileRecordUuid;
  DownloadLinkDto downloadLink;

  public static AvatarDto from(DownloadLinkDto downloadLink, Player player) {
    return AvatarDto.builder()
        .uuid(player.getUser().getUuid().toString())
        .fileRecordUuid(player.getAvatar().getUuid().toString())
        .downloadLink(downloadLink)
        .build();
  }
}
