package com.daytonjwatson.hardcore.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.daytonjwatson.hardcore.utils.Util;

public class BankTradeManager {

    private static BankTradeManager instance;

    private final Map<UUID, TradeSession> pendingTrades = new HashMap<>();
    private final Map<UUID, UUID> incomingByTarget = new HashMap<>();

    private BankTradeManager() {
    }

    public static void init() {
        if (instance == null) {
            instance = new BankTradeManager();
        }
    }

    public static BankTradeManager get() {
        return instance;
    }

    public void setPendingTrade(UUID sender, UUID target, ItemStack item, int returnPage) {
        if (item == null) return;
        ItemStack clone = item.clone();
        pendingTrades.put(sender, new TradeSession(sender, target, clone, returnPage, null, false, TradeState.SELECTING_PRICE));
    }

    public TradeSession getPendingTrade(UUID sender) {
        return pendingTrades.get(sender);
    }

    public void setAwaitingPrice(UUID sender, boolean awaiting) {
        TradeSession session = pendingTrades.get(sender);
        if (session == null) return;
        pendingTrades.put(sender,
                new TradeSession(session.sender(), session.target(), session.item(), session.returnPage(), session.price(), awaiting,
                        session.state()));
    }

    public boolean isAwaitingPrice(UUID sender) {
        TradeSession session = pendingTrades.get(sender);
        return session != null && session.awaitingPrice();
    }

    public void clear(UUID participant) {
        TradeSession bySender = pendingTrades.remove(participant);
        if (bySender != null) {
            incomingByTarget.remove(bySender.target());
        }

        UUID byTargetSender = incomingByTarget.remove(participant);
        if (byTargetSender != null) {
            pendingTrades.remove(byTargetSender);
        }
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

        if (!hasItem(sender.getInventory(), offered)) {
            sender.sendMessage(Util.color("&cYou no longer have that item to trade."));
            return false;
        }

        pendingTrades.put(sender.getUniqueId(), new TradeSession(sender.getUniqueId(), target.getUniqueId(), offered,
                session.returnPage(), price, false, TradeState.AWAITING_ACCEPT));
        incomingByTarget.put(target.getUniqueId(), sender.getUniqueId());
        return true;
    }

    public TradeSession getPendingForTarget(UUID target) {
        UUID sender = incomingByTarget.get(target);
        if (sender == null) return null;
        return pendingTrades.get(sender);
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

        clear(target.getUniqueId());
        return true;
    }

    public void declineTrade(UUID target) {
        TradeSession session = getPendingForTarget(target);
        if (session == null) return;

        Player sender = org.bukkit.Bukkit.getPlayer(session.sender());
        if (sender != null) {
            sender.sendMessage(Util.color("&cYour trade offer to &e" + session.targetName() + " &cwas declined."));
        }

        clear(target);
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

    private String formatItemName(ItemStack item) {
        String base = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : Util.formatMaterialName(item.getType());
        return base + " x" + item.getAmount();
    }

    public enum TradeState {SELECTING_PRICE, AWAITING_ACCEPT}

    public record TradeSession(UUID sender, UUID target, ItemStack item, int returnPage, Double price, boolean awaitingPrice,
                               TradeState state) {
        public String targetName() {
            Player online = org.bukkit.Bukkit.getPlayer(target);
            return online != null ? online.getName() : "player";
        }
    }
}
