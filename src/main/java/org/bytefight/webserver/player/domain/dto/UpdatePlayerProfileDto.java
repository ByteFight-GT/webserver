package org.bytefight.webserver.player.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePlayerProfileDto {
  @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
  @Pattern(
      regexp = "^[A-Za-z0-9_]+$",
      message = "Username can only contain letters, numbers, and underscores")
  private String username;

  @Size(max = 100)
  private String fullName;

  @Size(max = 512)
  private String description;

  @Size(max = 150)
  private String school;

  @Size(max = 256)
  private String major;

  @Size(max = 500)
  private String githubLink;

  @Size(max = 500)
  private String linkedinLink;

  @Size(max = 500)
  private String websiteLink;
}
