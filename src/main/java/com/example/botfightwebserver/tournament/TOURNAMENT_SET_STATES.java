package com.example.botfightwebserver.tournament;

public enum TOURNAMENT_SET_STATES {
    PENDING("pending"),
    OPEN("open"),
    COMPLETE("complete");

    private final String state;

    TOURNAMENT_SET_STATES(String state) {
        this.state = state;
    }

    public String toChallongeState() {
        return state;
    }

    public static TOURNAMENT_SET_STATES fromString(String text) {
        for (TOURNAMENT_SET_STATES state : TOURNAMENT_SET_STATES.values()) {
            if (state.state.equalsIgnoreCase(text)) {
                return state;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
