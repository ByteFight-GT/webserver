package org.bytefight.webserver.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupSurveyDto {
  @NotBlank(message = "Email is required.")
  @Email(message = "Email must be valid.")
  private String email;

  @NotBlank(message = "Please tell us how you heard about ByteFight.")
  private String heardAboutByteFight;
}
