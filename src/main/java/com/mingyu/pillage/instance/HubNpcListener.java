package com.mingyu.pillage.instance;

import com.mingyu.pillage.tp.TpManager;
import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubNpcListener implements Listener {

    private final JavaPlugin plugin;
    private final HubNpcManager npcManager;
    private final InstanceManager instanceManager;
    private final TpManager tpManager;

    public HubNpcListener(JavaPlugin plugin, HubNpcManager npcManager, InstanceManager instanceManager, TpManager tpManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.instanceManager = instanceManager;
        this.tpManager = tpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        HubNpcManager.Role role = npcManager.roleOf(event.getRightClicked());
        if (role == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (role) {
            case MAIN -> {
                if (tpManager.isTeleportBlocked(player)) return;
                instanceManager.teleportToMain(player);
                player.sendMessage(Msg.of("&a전체 약탈 서버로 이동했습니다."));
            }
            case MINI_CREATE -> promptCreate(player);
            case MINI_JOIN -> new InstanceSelectMenu(instanceManager, tpManager).open(player);
        }
    }

    // setInvulnerable(true) alone doesn't stop vanilla's /kill command - it's designed to bypass
    // invulnerability outright - so cancel damage explicitly too.
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (npcManager.roleOf(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    // Belt-and-suspenders: if something still removes an NPC despite the above (a command or
    // plugin that bypasses EntityDamageEvent entirely), respawn just the missing one next tick.
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (npcManager.roleOf(event.getEntity()) == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> npcManager.ensureSpawned(instanceManager.hubSpawn()));
    }

    private void promptCreate(Player player) {
        player.sendMessage(Msg.of("&a새 미니서버를 만들어봐요! 아래를 클릭하면 채팅창에 명령어가 채워집니다."));
        player.sendMessage(Component.text("[ 클릭해서 이름 입력하기 ]")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand("/mini create ")));
    }
}
