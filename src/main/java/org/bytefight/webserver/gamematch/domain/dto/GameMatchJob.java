package org.bytefight.webserver.gamematch.domain.dto;

import jakarta.validation.constraints.NotNull;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public class GameMatchJob implements Serializable {
    @NotNull private String gameMatchUuid;
    @NotNull private String competitionSlug;
    @NotNull private String teamAUuid;
    @NotNull private String teamBUuid;
    @NotNull private String submissionAUuid;
    @NotNull private String submissionBUuid;
    @NotNull private String ladder;
    @NotNull private MatchReason reason;
    @NotNull private Map<String, Object> matchSettings;

    public GameMatchJob() {
    }

    public GameMatchJob(
            String gameMatchUuid,
            String competitionSlug,
            String teamAUuid,
            String teamBUuid,
            String submissionAUuid,
            String submissionBUuid,
            String ladder,
            MatchReason reason,
            Map<String, Object> matchSettings
    ) {
        this.gameMatchUuid = gameMatchUuid;
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
        Object competition = getField(gameMatch, "competition");
        Object teamA = getField(gameMatch, "teamA");
        Object teamB = getField(gameMatch, "teamB");
        Object submissionA = getField(gameMatch, "submissionA");
        Object submissionB = getField(gameMatch, "submissionB");
        UUID matchUuid = getField(gameMatch, "uuid");
        String ladder = getField(gameMatch, "ladder");
        MatchReason reason = getField(gameMatch, "reason");
        Map<String, Object> matchSettings = getField(gameMatch, "matchSettings");

        String competitionSlug = competition != null ? getField(competition, "slug") : null;
        UUID teamAUuid = teamA != null ? getField(teamA, "uuid") : null;
        UUID teamBUuid = teamB != null ? getField(teamB, "uuid") : null;
        UUID submissionAUuid = submissionA != null ? getField(submissionA, "uuid") : null;
        UUID submissionBUuid = submissionB != null ? getField(submissionB, "uuid") : null;

        return new GameMatchJob(
                matchUuid != null ? matchUuid.toString() : null,
                competitionSlug,
                teamAUuid != null ? teamAUuid.toString() : null,
                teamBUuid != null ? teamBUuid.toString() : null,
                submissionAUuid != null ? submissionAUuid.toString() : null,
                submissionBUuid != null ? submissionBUuid.toString() : null,
                ladder,
                reason,
                matchSettings
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new IllegalStateException("Missing field " + name, ex);
        }
    }

    public String getGameMatchUuid() {
        return gameMatchUuid;
    }

    public void setGameMatchUuid(String gameMatchUuid) {
        this.gameMatchUuid = gameMatchUuid;
    }

    public String getCompetitionSlug() {
        return competitionSlug;
    }

    public void setCompetitionSlug(String competitionSlug) {
        this.competitionSlug = competitionSlug;
    }

    public String getTeamAUuid() {
        return teamAUuid;
    }

    public void setTeamAUuid(String teamAUuid) {
        this.teamAUuid = teamAUuid;
    }

    public String getTeamBUuid() {
        return teamBUuid;
    }

    public void setTeamBUuid(String teamBUuid) {
        this.teamBUuid = teamBUuid;
    }

    public String getSubmissionAUuid() {
        return submissionAUuid;
    }

    public void setSubmissionAUuid(String submissionAUuid) {
        this.submissionAUuid = submissionAUuid;
    }

    public String getSubmissionBUuid() {
        return submissionBUuid;
    }

    public void setSubmissionBUuid(String submissionBUuid) {
        this.submissionBUuid = submissionBUuid;
    }

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public MatchReason getReason() {
        return reason;
    }

    public void setReason(MatchReason reason) {
        this.reason = reason;
    }

    public Map<String, Object> getMatchSettings() {
        return matchSettings;
    }

    public void setMatchSettings(Map<String, Object> matchSettings) {
        this.matchSettings = matchSettings;
    }
}
