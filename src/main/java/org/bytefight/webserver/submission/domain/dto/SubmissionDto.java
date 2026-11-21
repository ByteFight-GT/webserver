package org.bytefight.webserver.submission.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchDto;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
  @NotNull private String uuid;
  @NotNull private String teamUuid;
  @NotNull private String name;
  @NotNull private SubmissionValidity validity;
  @NotNull private TimestampsDto timestampsDto;
  private GameMatchDto validationMatch;

  public static SubmissionDto from(Submission submission) {
    return from(submission, null);
  }

  public static SubmissionDto from(Submission submission, GameMatchDto validationMatch) {
    return SubmissionDto.builder()
        .uuid(submission.getUuid().toString())
        .teamUuid(submission.getTeam().getUuid().toString())
        .name(submission.getFileRecord().getFilename())
        .validity(submission.getValidity())
        .timestampsDto(TimestampsDto.from(submission))
        .validationMatch(validationMatch)
        .build();
  }
}
