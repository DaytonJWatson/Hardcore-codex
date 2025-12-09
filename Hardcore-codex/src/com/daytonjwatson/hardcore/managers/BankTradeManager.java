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
        pendingTrades.put(sender, new TradeSession(target, clone, returnPage, false));
    }

    public TradeSession getPendingTrade(UUID sender) {
        return pendingTrades.get(sender);
    }

    public void setAwaitingPrice(UUID sender, boolean awaiting) {
        TradeSession session = pendingTrades.get(sender);
        if (session == null) return;
        pendingTrades.put(sender, new TradeSession(session.target(), session.item(), session.returnPage(), awaiting));
    }

    public boolean isAwaitingPrice(UUID sender) {
        TradeSession session = pendingTrades.get(sender);
        return session != null && session.awaitingPrice();
    }

    public void clear(UUID sender) {
        pendingTrades.remove(sender);
    }

    public boolean executeTrade(Player sender, Player target, double price) {
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

        BankManager bank = BankManager.get();
        if (price > 0 && !bank.transfer(target.getUniqueId(), sender.getUniqueId(), price)) {
            sender.sendMessage(Util.color("&c" + target.getName() + " doesn't have enough balance to pay that price."));
            target.sendMessage(Util.color("&cYou don't have enough balance to buy that trade offer."));
            return false;
        }

        removeItem(sender.getInventory(), offered);
        HashMap<Integer, ItemStack> leftovers = target.getInventory().addItem(offered);
        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));

        String priceText = price > 0 ? bank.formatCurrency(price) : "free";
        sender.sendMessage(Util.color("&aTraded &f" + formatItemName(offered) + " &ato &e" + target.getName()
                + " &afor &f" + priceText + "&a."));
        target.sendMessage(Util.color("&aYou received &f" + formatItemName(offered) + " &afrom &e"
                + sender.getName() + "&a for &f" + priceText + "&a."));

        clear(sender.getUniqueId());
        return true;
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

    public record TradeSession(UUID target, ItemStack item, int returnPage, boolean awaitingPrice) {}
}
