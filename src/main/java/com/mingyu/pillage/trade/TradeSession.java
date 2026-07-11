package com.mingyu.pillage.trade;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class TradeSession implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int[] SLOTS_A = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    public static final int[] SLOTS_B = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    public static final int TOGGLE_A = 20;
    public static final int TOGGLE_B = 23;

    private final UUID playerA;
    private final UUID playerB;
    private final Inventory inventory;

    private boolean readyA;
    private boolean readyB;
    private boolean finished;

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.inventory = Bukkit.createInventory(this, SIZE, net.kyori.adventure.text.Component.text("물물교환"));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public UUID playerA() {
        return playerA;
    }

    public UUID playerB() {
        return playerB;
    }

    public boolean isParticipant(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }

    public UUID other(UUID uuid) {
        return playerA.equals(uuid) ? playerB : playerA;
    }

    public int[] rowOf(UUID uuid) {
        return playerA.equals(uuid) ? SLOTS_A : SLOTS_B;
    }

    public int toggleSlotOf(UUID uuid) {
        return playerA.equals(uuid) ? TOGGLE_A : TOGGLE_B;
    }

    public boolean isReady(UUID uuid) {
        return playerA.equals(uuid) ? readyA : readyB;
    }

    public void setReady(UUID uuid, boolean ready) {
        if (playerA.equals(uuid)) {
            readyA = ready;
        } else {
            readyB = ready;
        }
    }

    public boolean bothReady() {
        return readyA && readyB;
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished() {
        this.finished = true;
    }

    public ItemStack[] itemsOf(UUID uuid) {
        int[] slots = rowOf(uuid);
        ItemStack[] items = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            items[i] = inventory.getItem(slots[i]);
        }
        return items;
    }

    public Player playerAObj() {
        return Bukkit.getPlayer(playerA);
    }

    public Player playerBObj() {
        return Bukkit.getPlayer(playerB);
    }
}
