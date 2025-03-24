package com.example.botfightwebserver.tournament;

import java.util.Arrays;
import java.util.Random;

public enum TOURNEY_MAP {
    ARENA("arena"),
    LADDER("ladder"),
    COMPASSS("compasss"),
    RECURVE("recurve"),
    SSSPIRAL("ssspiral"),
    DIAMONDS("diamonds"),
    ATTRITION("attrition");

    private final String mapName;
    private static final Random random = new Random();

    TOURNEY_MAP(String mapName) {
        this.mapName = mapName;
    }

    public String toMapName() {
        return mapName;
    }

    public static TOURNEY_MAP fromMapName(String mapName) {
        for (TOURNEY_MAP map : TOURNEY_MAP.values()) {
            if (map.toMapName().equals(mapName)) {
                return map;
            }
        }
        throw new IllegalArgumentException("Invalid TOURNEY_MAPS: " + mapName);
    }

    public static TOURNEY_MAP getRandomMap() {
        TOURNEY_MAP[] maps = TOURNEY_MAP.values();
        return maps[random.nextInt(maps.length)];
    }
}
