package com.mingyu.pillage.tp;

import java.util.UUID;

public record TpRequest(UUID requester, long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
