package com.example.botfightwebserver.gameMatch;

public enum MAPS {
    PILLARS("pillars"),
    GREAT_DIVIDE("great_divide"),
    CAGE("cage"),
    EMPTY("empty");

    private final String mapName;

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
}
