package com.mingyu.pillage.instance;

import java.util.UUID;

/** One mini-server: its own world, its own fully-separate gameplay database, and an owner who
 *  is that instance's admin. */
public record InstanceInfo(String id, String name, UUID owner, String worldName, long createdAt) {
}
