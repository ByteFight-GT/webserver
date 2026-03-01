package org.bytefight.webserver.ladder.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

import org.bytefight.webserver.ladder.domain.Ladder;

@Value
@Builder
public class PublicLadderDto {
  @NotNull Long id;
  @NotNull String ladder;
  @NotNull String competitionSlug;
  @NotNull Long competitionId;

  public static PublicLadderDto from(Ladder ladder) {
    return PublicLadderDto.builder()
        .id(ladder.getId())
        .ladder(ladder.getLadder())
        .competitionId(ladder.getCompetition().getId())
        .competitionSlug(ladder.getCompetition().getSlug())
        .build();
  }

  public static List<PublicLadderDto> listFrom(List<Ladder> ladders) {
    return ladders.stream().map(PublicLadderDto::from).collect(Collectors.toList());
  }
}
