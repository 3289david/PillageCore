package com.mingyu.pillage.chat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ChatManager {

    private final int cooldownSeconds;
    private final boolean profanityFilterEnabled;
    private final Set<String> bannedWords;

    private final Map<UUID, Long> lastMessageAt = new HashMap<>();
    private final Map<UUID, UUID> lastWhisperFrom = new HashMap<>();

    public ChatManager(int cooldownSeconds, boolean profanityFilterEnabled, Set<String> bannedWords) {
        this.cooldownSeconds = cooldownSeconds;
        this.profanityFilterEnabled = profanityFilterEnabled;
        this.bannedWords = new HashSet<>(bannedWords);
    }

    public long remainingCooldownMillis(UUID uuid) {
        Long last = lastMessageAt.get(uuid);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long cooldownMillis = cooldownSeconds * 1000L;
        return Math.max(0, cooldownMillis - elapsed);
    }

    public void markMessaged(UUID uuid) {
        lastMessageAt.put(uuid, System.currentTimeMillis());
    }

    public String filter(String message) {
        if (!profanityFilterEnabled) return message;
        String result = message;
        for (String banned : bannedWords) {
            if (banned.isBlank()) continue;
            result = Pattern.compile(Pattern.quote(banned), Pattern.CASE_INSENSITIVE)
                    .matcher(result)
                    .replaceAll("*".repeat(banned.length()));
        }
        return result;
    }

    public void setLastWhisper(UUID target, UUID from) {
        lastWhisperFrom.put(target, from);
    }

    public UUID lastWhisperFrom(UUID target) {
        return lastWhisperFrom.get(target);
    }
}
