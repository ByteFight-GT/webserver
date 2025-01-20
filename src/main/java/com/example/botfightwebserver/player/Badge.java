package com.example.botfightwebserver.player;

public enum Badge {
    TOURNAMENT_WINNER(1, "Tournament Winner"),
    EARLY_ADOPTER(1 << 1, "Early Adopter"),
    TOP_CONTRIBUTOR(1 << 2, "Top Contributor");

    private final int bitFlag;
    private final String displayName;

    Badge(int bitFlag, String displayName) {
        this.bitFlag = bitFlag;
        this.displayName = displayName;
    }

    public int getBitFlag() {
        return bitFlag;
    }

    public String getDisplayName() {
        return displayName;
    }
}