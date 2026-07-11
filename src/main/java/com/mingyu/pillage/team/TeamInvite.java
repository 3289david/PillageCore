package com.mingyu.pillage.team;

import java.util.UUID;

public record TeamInvite(int teamId, UUID inviter, long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
