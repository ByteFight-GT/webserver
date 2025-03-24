package com.example.botfightwebserver.gameMatch;

import java.util.Random;

public enum MAPS {
    PILLARS("pillars"),
    GREAT_DIVIDE("great_divide"),
    CAGE("cage"),
    EMPTY("empty"),
    EMPTY_LARGE("empty_large"),
    SSSPLINE("ssspline"),
    COMBUSTIBLE_LEMONS("combustible_lemons"),
    ARENA("arena"),
    LADDER("ladder"),
    COMPASSS("compasss"),
    RECURVE("recurve"),
    SSSPIRAL("ssspiral"),
    DIAMONDS("diamonds"),
    ATTRITION("attrition");

    private final String mapName;
    private static final Random random = new Random();

    MAPS(String mapName) {
        this.mapName = mapName;
    }

    public String toMapName() {
        return mapName;
    }

    public static MAPS fromMapName(String mapName) {
        for (MAPS m : MAPS.values()) {
            if (m.mapName.equals(mapName)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Invalid map name: " + mapName);
    }

    public static MAPS getRandomMap() {
        MAPS[] maps = MAPS.values();
        return maps[random.nextInt(maps.length)];
    }
}
