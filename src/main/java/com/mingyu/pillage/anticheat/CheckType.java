package com.mingyu.pillage.anticheat;

public enum CheckType {
    KILLAURA("KillAura (시야)"),
    REACH("Reach"),
    SPEED("Speed"),
    FLY("Fly"),
    AUTOCLICK("AutoClick (CPS)"),
    SCAFFOLD("Scaffold"),
    FASTBREAK("FastBreak");

    private final String displayName;

    CheckType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
