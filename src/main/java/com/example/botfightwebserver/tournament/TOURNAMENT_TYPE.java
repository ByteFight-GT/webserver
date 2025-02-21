package com.example.botfightwebserver.tournament;

public enum TOURNAMENT_TYPE {
    SINGLE_ELIMINATION("single elimination"),
    DOUBLE_ELIMINATION("double elimination"),
    ROUND_ROBIN("round robin"),
    SWISS("swiss");

    private final String challongeType;

    TOURNAMENT_TYPE(String challongeType) {
        this.challongeType = challongeType;
    }

    public String toChallongeType() {
        return this.challongeType;
    }

    public static TOURNAMENT_TYPE fromChallongeType(String challongeType) {
        for (TOURNAMENT_TYPE tournamentType : TOURNAMENT_TYPE.values()) {
            if (tournamentType.toChallongeType().equals(challongeType)) {
                return tournamentType;
            }
        }
        throw new IllegalArgumentException(challongeType + " is not a valid TOURNAMENT_TYPE");
    }
}