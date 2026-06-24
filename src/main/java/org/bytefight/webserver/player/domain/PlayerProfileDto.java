package org.bytefight.webserver.player.domain;

import java.util.List;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlayerProfileDto {
  @NotNull String uuid;
  @NotNull String username;

  String fullName;
  String description;
  String major;
  Integer graduationYear;
  String school;
  String avatarUrl;

  @NotNull TimestampsDto timestampsDto;
  @NotNull List<SocialLinkDto> socialLinks;

  public static PlayerProfileDto from(Player player) {
    return PlayerProfileDto.builder()
      .uuid(player.getUser().getUuid().toString())
      .username(player.getUsername())
      .fullName(player.getFullName())
      .description(player.getDescription())
      .major(player.getMajor())
      .graduationYear(player.getGraduationYear())
      .school(player.getSchool())
      .avatarUrl(null)
      .timestampsDto(TimestampsDto.from(player))
      .socialLinks(player.getSocialLinks().stream().map(SocialLinkDto::from).toList())
      .build();
  }
}