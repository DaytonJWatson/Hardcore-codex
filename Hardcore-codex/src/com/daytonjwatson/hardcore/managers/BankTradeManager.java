package com.daytonjwatson.hardcore.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.utils.Util;

public class BankTradeManager {

    private static BankTradeManager instance;

    private static final int MAX_PENDING_OFFERS = 1;
    private static final long EXPIRATION_MILLIS = 15 * 60 * 1000L;

    private final Map<UUID, TradeSession> pendingTrades = new HashMap<>();
    private final Map<UUID, List<UUID>> incomingByTarget = new HashMap<>();

    private BankTradeManager() {
    }

    public static void init() {
        if (instance == null) {
            instance = new BankTradeManager();
            instance.startCleanupTask();
        }
    }

    public static BankTradeManager get() {
        return instance;
    }

    public void setPendingTrade(UUID sender, UUID target, ItemStack item, int returnPage) {
        if (item == null) return;
        ItemStack clone = item.clone();
        pendingTrades.put(sender,
                new TradeSession(sender, target, clone, returnPage, null, false, TradeState.SELECTING_PRICE,
                        System.currentTimeMillis()));
    }

    public TradeSession getPendingTrade(UUID sender) {
        TradeSession session = pendingTrades.get(sender);
        if (session == null) return null;
        if (isExpired(session)) {
            removeBySender(sender);
            return null;
        }
        return session;
    }

    public void setAwaitingPrice(UUID sender, boolean awaiting) {
        TradeSession session = pendingTrades.get(sender);
        if (session == null) return;
        pendingTrades.put(sender,
                new TradeSession(session.sender(), session.target(), session.item(), session.returnPage(), session.price(), awaiting,
                        session.state(), System.currentTimeMillis()));
    }

    public boolean isAwaitingPrice(UUID sender) {
        TradeSession session = pendingTrades.get(sender);
        return session != null && session.awaitingPrice();
    }

    public void clear(UUID participant) {
        cleanupExpired();

        removeBySender(participant);

        List<UUID> toRemove = new ArrayList<>();
        for (TradeSession session : pendingTrades.values()) {
            if (session.target().equals(participant)) {
                toRemove.add(session.sender());
            }
        }
        toRemove.forEach(this::removeBySender);
    }

    public boolean sendOffer(Player sender, Player target, double price) {
        TradeSession session = pendingTrades.get(sender.getUniqueId());
        if (session == null) {
            sender.sendMessage(Util.color("&cNo trade data found. Please open the trade menu again."));
            return false;
        }

        ItemStack offered = session.item();
        if (offered == null || offered.getType().isAir()) {
            sender.sendMessage(Util.color("&cYou must offer an item from your hand."));
            return false;
        }

        if (target == null || !target.isOnline()) {
            sender.sendMessage(Util.color("&cThat player is no longer online."));
            return false;
        }

        int pendingCount = getPendingCount(target.getUniqueId());
        if (pendingCount >= MAX_PENDING_OFFERS) {
            sender.sendMessage(Util.color("&cThat player already has a pending trade offer. Try again later."));
            return false;
        }

        if (!hasItem(sender.getInventory(), offered)) {
            sender.sendMessage(Util.color("&cYou no longer have that item to trade."));
            return false;
        }

        pendingTrades.put(sender.getUniqueId(), new TradeSession(sender.getUniqueId(), target.getUniqueId(), offered,
                session.returnPage(), price, false, TradeState.AWAITING_ACCEPT, System.currentTimeMillis()));
        List<UUID> incoming = incomingByTarget.computeIfAbsent(target.getUniqueId(), id -> new ArrayList<>());
        if (!incoming.contains(sender.getUniqueId())) {
            incoming.add(sender.getUniqueId());
        }
        return true;
    }

    public TradeSession getPendingForTarget(UUID target) {
        List<TradeSession> sessions = getPendingOffers(target);
        if (sessions.isEmpty()) return null;
        return sessions.get(0);
    }

    public List<TradeSession> getPendingOffers(UUID target) {
        cleanupExpired();
        pruneInvalidForTarget(target);

        List<TradeSession> active = new ArrayList<>();
        List<UUID> senders = new ArrayList<>();

        for (Map.Entry<UUID, TradeSession> entry : pendingTrades.entrySet()) {
            TradeSession session = entry.getValue();
            if (session == null || !session.target().equals(target) || isExpired(session)) {
                continue;
            }
            if (session.state() != TradeState.AWAITING_ACCEPT) {
                continue;
            }
            active.add(session);
            senders.add(entry.getKey());
        }

        if (senders.isEmpty()) {
            incomingByTarget.remove(target);
        } else {
            incomingByTarget.put(target, senders);
        }

        return active;
    }

    public int getPendingCount(UUID target) {
        return getPendingOffers(target).size();
    }

    public boolean acceptTrade(Player target) {
        TradeSession session = getPendingForTarget(target.getUniqueId());
        if (session == null || session.state() != TradeState.AWAITING_ACCEPT) {
            target.sendMessage(Util.color("&cNo trade request to accept."));
            return false;
        }

        Player sender = org.bukkit.Bukkit.getPlayer(session.sender());
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(Util.color("&cThat player is no longer online."));
            clear(target.getUniqueId());
            return false;
        }

        ItemStack offered = session.item();
        if (!hasItem(sender.getInventory(), offered)) {
            target.sendMessage(Util.color("&cThe sender no longer has that item to trade."));
            sender.sendMessage(Util.color("&cTrade cancelled because you no longer have the item."));
            clear(target.getUniqueId());
            return false;
        }

        BankManager bank = BankManager.get();
        double price = session.price() == null ? 0 : session.price();
        if (price > 0 && !bank.transfer(target.getUniqueId(), sender.getUniqueId(), price)) {
            target.sendMessage(Util.color("&cYou don't have enough balance to accept that trade."));
            sender.sendMessage(Util.color("&c" + target.getName() + " couldn't afford the trade."));
            return false;
        }

        removeItem(sender.getInventory(), offered);
        HashMap<Integer, ItemStack> leftovers = target.getInventory().addItem(offered);
        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));

        String priceText = price > 0 ? bank.formatCurrency(price) : "free";
        sender.sendMessage(Util.color("&aYour trade with &e" + target.getName() + " &awas accepted for &f" + priceText + "&a."));
        target.sendMessage(Util.color("&aYou accepted &f" + formatItemName(offered) + " &afrom &e" + sender.getName()
                + " &afor &f" + priceText + "&a."));

        removeBySender(session.sender());
        return true;
    }

    public void declineTrade(UUID target) {
        TradeSession session = getPendingForTarget(target);
        if (session == null) return;

        Player sender = org.bukkit.Bukkit.getPlayer(session.sender());
        if (sender != null) {
            sender.sendMessage(Util.color("&cYour trade offer to &e" + session.targetName() + " &cwas declined."));
        }

        removeBySender(session.sender());
    }

    private boolean hasItem(PlayerInventory inventory, ItemStack item) {
        int needed = item.getAmount();
        for (ItemStack content : inventory.getContents()) {
            if (content == null) continue;
            if (content.isSimilar(item)) {
                needed -= content.getAmount();
            }
            if (needed <= 0) {
                return true;
            }
        }
        return false;
    }

    private void removeItem(PlayerInventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null) continue;
            if (!content.isSimilar(item)) continue;

            int remove = Math.min(remaining, content.getAmount());
            content.setAmount(content.getAmount() - remove);
            if (content.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= remove;
            if (remaining <= 0) {
                break;
            }
        }
        inventory.setContents(contents);
    }

    public String formatItemName(ItemStack item) {
        String base = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : Util.formatMaterialName(item.getType());
        return base + " x" + item.getAmount();
    }

    public enum TradeState {SELECTING_PRICE, AWAITING_ACCEPT}

    public record TradeSession(UUID sender, UUID target, ItemStack item, int returnPage, Double price, boolean awaitingPrice,
                               TradeState state, long createdAt) {
        public String targetName() {
            Player online = org.bukkit.Bukkit.getPlayer(target);
            return online != null ? online.getName() : "player";
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpired();
            }
        }.runTaskTimer(HardcorePlugin.getInstance(), 20L * 60, 20L * 60);
    }

    private void cleanupExpired() {
        List<UUID> toRemove = new ArrayList<>();
        for (TradeSession session : pendingTrades.values()) {
            if (isExpired(session)) {
                toRemove.add(session.sender());
            }
        }
        toRemove.forEach(this::removeBySender);
    }

    private void pruneInvalidForTarget(UUID target) {
        List<UUID> senders = incomingByTarget.get(target);
        if (senders == null || senders.isEmpty()) {
            return;
        }

        List<UUID> copy = new ArrayList<>(senders);
        for (UUID sender : copy) {
            TradeSession session = pendingTrades.get(sender);
            if (session == null || !session.target().equals(target) || isExpired(session)) {
                removeBySender(sender);
            }
        }
    }

    private boolean isExpired(TradeSession session) {
        return System.currentTimeMillis() - session.createdAt() > EXPIRATION_MILLIS;
    }

    private void removeBySender(UUID sender) {
        TradeSession removed = pendingTrades.remove(sender);
        if (removed == null) return;

        List<UUID> incoming = incomingByTarget.get(removed.target());
        if (incoming != null) {
            incoming.remove(sender);
            if (incoming.isEmpty()) {
                incomingByTarget.remove(removed.target());
            }
        }
    }
}
