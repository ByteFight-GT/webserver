package com.example.botfightwebserver.tournament_cursor.domain;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import lombok.Builder;
import lombok.Getter;

/**
 * Read-only match node view for bracket/timeline rendering.
 * Includes linkage to next matches and optional GameMatch UUID.
 */
@Getter
@Builder
public class TournamentMatchDto {
    private final Long matchId;
    private final String uuid;
    private final TournamentBracketType bracketType;
    private final Integer roundNumber;
    private final Integer matchIndex;
    private final Long teamOneEntryId;
    private final String teamOneUuid;
    private final String teamOneName;
    private final Long teamTwoEntryId;
    private final String teamTwoUuid;
    private final String teamTwoName;
    private final TournamentMatchState state;
    private final String gameMatchUuid;
    private final Long winnerEntryId;
    private final Long loserEntryId;
    private final Long nextWinnerMatchId;
    private final Integer nextWinnerSlot;
    private final Long nextLoserMatchId;
    private final Integer nextLoserSlot;

    public static TournamentMatchDto from(TournamentMatch match) {
        GameMatch gameMatch = match.getGameMatch();
        var teamOne = match.getTeamOneEntry() != null ? match.getTeamOneEntry().getTeam() : null;
        var teamTwo = match.getTeamTwoEntry() != null ? match.getTeamTwoEntry().getTeam() : null;

        return TournamentMatchDto.builder()
                .matchId(match.getId())
                .uuid(match.getUuid().toString())
                .bracketType(match.getBracketType())
                .roundNumber(match.getRoundNumber())
                .matchIndex(match.getMatchIndex())
                .teamOneEntryId(match.getTeamOneEntry() != null ? match.getTeamOneEntry().getId() : null)
                .teamOneUuid(teamOne != null ? teamOne.getUuid().toString() : null)
                .teamOneName(teamOne != null ? teamOne.getName() : null)
                .teamTwoEntryId(match.getTeamTwoEntry() != null ? match.getTeamTwoEntry().getId() : null)
                .teamTwoUuid(teamTwo != null ? teamTwo.getUuid().toString() : null)
                .teamTwoName(teamTwo != null ? teamTwo.getName() : null)
                .state(match.getState())
                .gameMatchUuid(gameMatch != null ? gameMatch.getUuid().toString() : null)
                .winnerEntryId(match.getWinnerEntry() != null ? match.getWinnerEntry().getId() : null)
                .loserEntryId(match.getLoserEntry() != null ? match.getLoserEntry().getId() : null)
                .nextWinnerMatchId(match.getNextWinnerMatchId())
                .nextWinnerSlot(match.getNextWinnerSlot())
                .nextLoserMatchId(match.getNextLoserMatchId())
                .nextLoserSlot(match.getNextLoserSlot())
                .build();
    }
}
