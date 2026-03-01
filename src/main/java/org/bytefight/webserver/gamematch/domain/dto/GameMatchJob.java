package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;

@Getter
public class GameMatchJob implements Serializable {
  @NotNull private String uuid;
  @NotNull private String competitionSlug;
  @NotNull private String teamAUuid;
  @NotNull private String teamBUuid;
  @NotNull private String submissionAUuid;
  @NotNull private String submissionBUuid;
  @NotNull private String ladder;
  @NotNull private MatchReason reason;
  @NotNull private Map<String, Object> matchSettings;

  public GameMatchJob() {}

  public GameMatchJob(
      String uuid,
      String competitionSlug,
      String teamAUuid,
      String teamBUuid,
      String submissionAUuid,
      String submissionBUuid,
      String ladder,
      MatchReason reason,
      Map<String, Object> matchSettings) {
    this.uuid = uuid;
    this.competitionSlug = competitionSlug;
    this.teamAUuid = teamAUuid;
    this.teamBUuid = teamBUuid;
    this.submissionAUuid = submissionAUuid;
    this.submissionBUuid = submissionBUuid;
    this.ladder = ladder;
    this.reason = reason;
    this.matchSettings = matchSettings;
  }

  public static GameMatchJob from(GameMatch gameMatch) {
    return new GameMatchJob(
        gameMatch.getUuid().toString(),
        gameMatch.getCompetition().getSlug(),
        gameMatch.getTeamA().getUuid().toString(),
        gameMatch.getTeamB().getUuid().toString(),
        gameMatch.getSubmissionA().getUuid().toString(),
        gameMatch.getSubmissionB().getUuid().toString(),
        gameMatch.getLadder(),
        gameMatch.getReason(),
        gameMatch.getMatchSettings());
  }
}
