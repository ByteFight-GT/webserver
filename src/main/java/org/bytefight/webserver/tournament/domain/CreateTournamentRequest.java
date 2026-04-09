package org.bytefight.webserver.tournament.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Admin input payload for creating and enrolling a tournament. */
@Getter
@Setter
public class CreateTournamentRequest {
  @NotBlank private String name;

  private List<String> teamUuids;
  private String seedLadder;
}
