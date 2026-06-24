package org.bytefight.webserver.player.domain;

import java.util.List;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePlayerProfileDto {
  @PlayerUsername private String username;
  @Size(max = 100)
  private String fullName;

  @Size(max = 512)
  private String description;

  @Size(max = 256)
  private String major;

  @PositiveOrZero
  private Integer graduationYear;

  @Size(max = 150)
  private String school;

  private ProfileVisibility profileVisibility;

  private List<SocialLinkDto> socialLinks;
}
