package org.bytefight.webserver.player.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.player.domain.Player;

@Value
@Builder
public class PublicPlayerDto {
  @NotNull String uuid;
  @NotNull String username;
  String fullName;
  String description;
  String school;
  String major;
  String githubLink;
  String linkedinLink;
  String websiteLink;
  @NotNull TimestampsDto timestampsDto;
  @NotNull boolean isDev;

  public static PublicPlayerDto from(Player player) {
    return PublicPlayerDto.builder()
        .uuid(player.getUser().getUuid().toString())
        .username(player.getUsername())
        .fullName(player.getFullName())
        .description(player.getDescription())
        .school(player.getSchool())
        .major(player.getMajor())
        .githubLink(player.getGithubLink())
        .linkedinLink(player.getLinkedinLink())
        .websiteLink(player.getWebsiteLink())
        .timestampsDto(TimestampsDto.from(player))
        .isDev(player.isDev())
        .build();
  }
}
