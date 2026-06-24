package org.bytefight.webserver.player.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SocialLinkDto {
  @NotNull SocialPlatform platform;
  @NotNull String url;

  public static SocialLinkDto from(SocialLink link) {
    return SocialLinkDto.builder()
      .platform(link.getPlatform())
      .url(link.getUrl())
      .build();
  }
}