package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.bytefight.webserver.gamematch.domain.MatchReason;

@Value
public class AdminCreateMatchDto {
    @NotNull Long competitionId;
    @NotNull Long ladderId;
    @NotNull Long teamAId;
    @NotNull Long teamBId;
    @NotNull Long teamASubmissionId;
    @NotNull Long teamBSubmissionId;
    @NotNull MatchReason reason;
}
