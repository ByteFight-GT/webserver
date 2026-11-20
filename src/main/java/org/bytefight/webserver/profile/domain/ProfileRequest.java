package org.bytefight.webserver.profile.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
  @Size(max = 512)
  private String description;

  @Size(max = 256)
  private String major;

  @Size(max = 150)
  private String school;

  @Min(1900)
  @Max(2200)
  private Integer graduationYear;
}
