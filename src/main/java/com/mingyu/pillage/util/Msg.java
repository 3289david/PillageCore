package com.mingyu.pillage.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Msg() {
    }

    public static Component of(String raw) {
        return LEGACY.deserialize(raw);
    }

    public static Component of(String raw, String... placeholderPairs) {
        String text = raw;
        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            text = text.replace(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return LEGACY.deserialize(text);
    }
}
