package org.bytefight.webserver.submission.domain.dto;

import lombok.Value;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;

@Value
public class AdminSubmissionDto {
  Long id;
  String uuid;
  Long teamId;
  String teamUuid;
  Long competitionId;
  String description;
  String filename;
  SubmissionValidity validity;
  boolean isDeleted;
  TimestampsDto timestamps;

  public static AdminSubmissionDto from(Submission submission) {
    return new AdminSubmissionDto(
        submission.getId(),
        submission.getUuid().toString(),
        submission.getTeam().getId(),
        submission.getTeam().getUuid().toString(),
        submission.getTeam().getCompetition().getId(),
        submission.getDescription(),
        submission.getFileRecord().getFilename(),
        submission.getValidity(),
        submission.isDeleted(),
        TimestampsDto.from(submission));
  }
}
