package com.mingyu.pillage.trade;

import com.mingyu.pillage.data.dao.TradeLogDao;
import com.mingyu.pillage.util.ItemBuilder;
import com.mingyu.pillage.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TradeManager {

    private final TradeLogDao tradeLogDao;
    private final Map<UUID, TradeRequest> incomingRequests = new HashMap<>();
    private final Map<UUID, TradeSession> sessionsByPlayer = new HashMap<>();

    public TradeManager(TradeLogDao tradeLogDao) {
        this.tradeLogDao = tradeLogDao;
    }

    private record TradeRequest(UUID requester, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public void sendRequest(Player requester, Player target) {
        incomingRequests.put(target.getUniqueId(),
                new TradeRequest(requester.getUniqueId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60)));
        target.sendMessage(Msg.of("&e" + requester.getName() + "&f 님이 거래를 요청했습니다. &a/tradeaccept &f또는 &c/tradedeny"));
        requester.sendMessage(Msg.of("&f" + target.getName() + " 님에게 거래 요청을 보냈습니다."));
    }

    public void denyRequest(UUID target) {
        incomingRequests.remove(target);
    }

    public UUID peekRequester(UUID target) {
        TradeRequest request = incomingRequests.get(target);
        if (request == null || request.isExpired()) {
            return null;
        }
        return request.requester();
    }

    public boolean isInTrade(UUID uuid) {
        return sessionsByPlayer.containsKey(uuid);
    }

    public TradeSession sessionOf(UUID uuid) {
        return sessionsByPlayer.get(uuid);
    }

    public void cancelAll(String reason) {
        for (TradeSession session : new java.util.HashSet<>(sessionsByPlayer.values())) {
            cancelTrade(session, reason);
        }
    }

    public enum AcceptResult { OK, NO_REQUEST, REQUESTER_OFFLINE, ALREADY_TRADING }

    public AcceptResult acceptRequest(Player target) {
        TradeRequest request = incomingRequests.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            return AcceptResult.NO_REQUEST;
        }
        Player requester = org.bukkit.Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            return AcceptResult.REQUESTER_OFFLINE;
        }
        if (isInTrade(requester.getUniqueId()) || isInTrade(target.getUniqueId())) {
            return AcceptResult.ALREADY_TRADING;
        }
        openSession(requester, target);
        return AcceptResult.OK;
    }

    private void openSession(Player a, Player b) {
        TradeSession session = new TradeSession(a.getUniqueId(), b.getUniqueId());
        sessionsByPlayer.put(a.getUniqueId(), session);
        sessionsByPlayer.put(b.getUniqueId(), session);

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7").build();
        for (int slot = 18; slot <= 26; slot++) {
            session.getInventory().setItem(slot, filler);
        }
        refreshToggleIcon(session, session.playerA());
        refreshToggleIcon(session, session.playerB());

        a.openInventory(session.getInventory());
        b.openInventory(session.getInventory());
    }

    public void refreshToggleIcon(TradeSession session, UUID uuid) {
        boolean ready = session.isReady(uuid);
        ItemStack icon = ready
                ? new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&a확인함 (클릭하여 취소)").build()
                : new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&c확인 대기중 (클릭하여 확인)").build();
        session.getInventory().setItem(session.toggleSlotOf(uuid), icon);
    }

    public void completeTrade(TradeSession session) {
        session.markFinished();
        sessionsByPlayer.remove(session.playerA());
        sessionsByPlayer.remove(session.playerB());

        Player a = session.playerAObj();
        Player b = session.playerBObj();

        ItemStack[] itemsA = session.itemsOf(session.playerA());
        ItemStack[] itemsB = session.itemsOf(session.playerB());

        if (a != null) {
            for (ItemStack item : itemsB) {
                giveOrDrop(a, item);
            }
        }
        if (b != null) {
            for (ItemStack item : itemsA) {
                giveOrDrop(b, item);
            }
        }

        tradeLogDao.log(session.playerA(), session.playerB(), summarize(itemsA), summarize(itemsB));

        if (a != null) a.sendMessage(Msg.of("&a거래가 완료되었습니다."));
        if (b != null) b.sendMessage(Msg.of("&a거래가 완료되었습니다."));
    }

    public void cancelTrade(TradeSession session, String reason) {
        if (session.isFinished()) return;
        session.markFinished();
        sessionsByPlayer.remove(session.playerA());
        sessionsByPlayer.remove(session.playerB());

        Player a = session.playerAObj();
        Player b = session.playerBObj();

        for (ItemStack item : session.itemsOf(session.playerA())) {
            if (a != null) giveOrDrop(a, item);
        }
        for (ItemStack item : session.itemsOf(session.playerB())) {
            if (b != null) giveOrDrop(b, item);
        }

        if (a != null) {
            a.closeInventory();
            a.sendMessage(Msg.of("&c거래가 취소되었습니다. (" + reason + ")"));
        }
        if (b != null) {
            b.closeInventory();
            b.sendMessage(Msg.of("&c거래가 취소되었습니다. (" + reason + ")"));
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        var leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    private String summarize(ItemStack[] items) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(item.getType()).append(" x").append(item.getAmount());
        }
        return sb.toString();
    }
}
