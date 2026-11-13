package org.bytefight.webserver.user.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin edit of a user. Only the fields listed here can change; anything else in the body (id,
 * email, admin flag, timestamps, teams, ...) is ignored, since react-admin sends the whole record.
 *
 * <p>A null top-level flag leaves it unchanged. If {@code player} is present, its text fields
 * replace the stored values exactly (null or blank clears them), and {@code username} is required.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminUpdateUserDto(
    Boolean serviceAccount, Boolean internalCompEligible, @Valid PlayerUpdate player) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlayerUpdate(
      @NotBlank(message = "Username is required")
          @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
          @Pattern(
              regexp = "^[A-Za-z0-9_]+$",
              message = "Username can only contain letters, numbers, and underscores")
          String username,
      @Size(max = 100) String fullName,
      @Size(max = 512) String description,
      @Size(max = 150) String school,
      @Size(max = 256) String major,
      @Size(max = 500) String githubLink,
      @Size(max = 500) String linkedinLink,
      @Size(max = 500) String websiteLink,
      Boolean dev) {}
}
