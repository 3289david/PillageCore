package com.mingyu.pillage.instance;

import com.mingyu.pillage.menu.PillageMenu;
import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Opened by the "미니게임 참가" hub NPC (or reachable via /mini list + join) - lets a player pick
 *  an existing mini-server to join by clicking it instead of typing its name. */
public final class InstanceSelectMenu implements PillageMenu {

    private final InstanceManager instanceManager;
    private final TpManager tpManager;
    private final Inventory inventory;
    private final List<InstanceInfo> instances;

    public InstanceSelectMenu(InstanceManager instanceManager, TpManager tpManager) {
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
        this.instances = instanceManager.list();
        int size = Math.min(54, Math.max(9, ((instances.size() + 8) / 9) * 9));
        this.inventory = Bukkit.createInventory(this, size, Component.text("미니서버 참가"));
        render();
    }

    private void render() {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, ItemBuilder.filler());
        }
        for (int i = 0; i < instances.size() && i < inventory.getSize(); i++) {
            InstanceInfo info = instances.get(i);
            String ownerName = Bukkit.getOfflinePlayer(info.owner()).getName();
            inventory.setItem(i, new ItemBuilder(Material.GRASS_BLOCK)
                    .name("&a" + info.name())
                    .lore("&7관리자: " + (ownerName == null ? "???" : ownerName), "&e클릭하여 입장")
                    .build());
        }
    }

    public void open(Player player) {
        if (instances.isEmpty()) {
            player.sendMessage(Msg.of("&7아직 생성된 미니서버가 없습니다. &e/mini create <이름>&7 으로 만들어보세요."));
            return;
        }
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot < 0 || slot >= instances.size()) return;
        InstanceInfo info = instances.get(slot);
        if (tpManager.isTeleportBlocked(player)) {
            player.closeInventory();
            return;
        }
        player.closeInventory();
        instanceManager.teleportToInstance(player, info);
        player.sendMessage(Msg.of("&a미니서버 '" + info.name() + "' 에 입장했습니다."));
    }
}
