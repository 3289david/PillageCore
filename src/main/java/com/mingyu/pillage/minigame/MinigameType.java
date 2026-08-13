package com.mingyu.pillage.minigame;

public enum MinigameType {
    SPLEEF("스플레프", 2),
    TNT_RUN("TNT 런", 2),
    PARKOUR("파쿠르 레이스", 1),
    TAG("술래잡기", 2);

    private final String displayName;
    private final int minPlayers;

    MinigameType(String displayName, int minPlayers) {
        this.displayName = displayName;
        this.minPlayers = minPlayers;
    }

    public String displayName() {
        return displayName;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public static MinigameType fromArg(String arg) {
        for (MinigameType type : values()) {
            if (type.name().equalsIgnoreCase(arg) || type.name().replace("_", "").equalsIgnoreCase(arg)) {
                return type;
            }
        }
        return null;
    }
}
