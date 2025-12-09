package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.managers.BankTradeManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class BankTradeGui {

    public static final String SELECT_TITLE = Util.color("&6&lBank &8| &7Trade Partner");
    public static final String OFFER_TITLE_PREFIX = Util.color("&6&lBank &8| &7Trade with ");

    private static final NamespacedKey TARGET_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_trade_target");
    private static final NamespacedKey PRICE_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_trade_price");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_trade_page");
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private BankTradeGui() {}

    public static void openRecipientSelection(Player player) {
        openRecipientSelection(player, 0);
    }

    public static void openRecipientSelection(Player player, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        online.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int totalPages = Math.max(1, (int) Math.ceil(online.size() / (double) CONTENT_SLOTS.length));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory menu = Bukkit.createInventory(null, 54, SELECT_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int startIndex = currentPage * CONTENT_SLOTS.length;
        for (int i = startIndex, slotIndex = 0; i < Math.min(online.size(), startIndex + CONTENT_SLOTS.length);
                i++, slotIndex++) {
            Player target = online.get(i);
            ItemStack head = playerHead(target, List.of(
                    "&7Select a player to send your item to.",
                    "&7They'll pay using their bank balance.",
                    "&8You'll confirm the price next."
            ));
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, currentPage);
                head.setItemMeta(meta);
            }
            menu.setItem(CONTENT_SLOTS[slotIndex], head);
        }

        if (online.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&cNo players online", List.of(
                    "&7Ask a friend to join before trading!"));
            menu.setItem(22, empty);
        }

        ItemStack back = attachPage(item(Material.BARRIER, "&cBack", List.of("&7Return to bank menu")), 0);
        menu.setItem(47, back);
        addNavigation(menu, currentPage, totalPages);

        player.openInventory(menu);
    }

    public static void openOfferConfirm(Player sender, Player target, int fromPage) {
        ItemStack hand = sender.getInventory().getItemInMainHand();
        String targetName = target.getName() != null ? target.getName() : "player";
        String title = OFFER_TITLE_PREFIX + Util.color("&e" + targetName);

        Inventory menu = Bukkit.createInventory(null, 27, title);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        if (hand == null || hand.getType().isAir()) {
            ItemStack warning = item(Material.BARRIER, "&cNo item held", List.of(
                    "&7Hold the item you want to trade in your main hand.",
                    "&7Re-open this menu once you're ready."
            ));
            menu.setItem(13, warning);
            ItemStack back = attachPage(item(Material.ARROW, "&7Choose different player", List.of("&7Return to player list")), fromPage);
            menu.setItem(18, back);
            sender.openInventory(menu);
            return;
        }

        BankTradeManager.get().setPendingTrade(sender.getUniqueId(), target.getUniqueId(), hand, fromPage);

        ItemStack preview = hand.clone();
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            List<String> lore = previewMeta.hasLore() ? new ArrayList<>(previewMeta.getLore()) : new ArrayList<>();
            lore.add(Util.color("&7Trading to: &f" + targetName));
            previewMeta.setLore(lore);
            preview.setItemMeta(previewMeta);
        }
        menu.setItem(13, preview);

        double[] prices = {0, 100, 500, 1000};
        int[] slots = {10, 11, 12, 14};
        for (int i = 0; i < prices.length; i++) {
            String label = prices[i] <= 0 ? "&aSend for Free" : "&eCharge " + BankManager.get().formatCurrency(prices[i]);
            ItemStack option = attachPage(amountItem(prices[i], target.getUniqueId(), label), fromPage);
            menu.setItem(slots[i], option);
        }

        ItemStack custom = item(Material.NAME_TAG, "&eEnter Custom Price", List.of(
                "&7Click to type a price in chat.",
                "&7Type &ccancel &7to abort the trade.",
                "&8Money moves through the bank instantly."));
        ItemMeta customMeta = custom.getItemMeta();
        if (customMeta != null) {
            customMeta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
            customMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, fromPage);
            custom.setItemMeta(customMeta);
        }
        menu.setItem(22, custom);

        ItemStack back = attachPage(item(Material.ARROW, "&7Choose different player", List.of("&7Go back to player list")), fromPage);
        menu.setItem(18, back);

        sender.openInventory(menu);
    }

    public static boolean isTradeInventory(String title) {
        String stripped = org.bukkit.ChatColor.stripColor(title);
        return stripped.equals(org.bukkit.ChatColor.stripColor(SELECT_TITLE))
                || stripped.startsWith(org.bukkit.ChatColor.stripColor(OFFER_TITLE_PREFIX));
    }

    public static UUID getTargetFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Double getPriceFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        Double stored = item.getItemMeta().getPersistentDataContainer().get(PRICE_KEY, PersistentDataType.DOUBLE);
        if (stored == null) return null;
        return stored;
    }

    public static Integer getPageFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(PAGE_KEY, PersistentDataType.INTEGER);
    }

    private static ItemStack amountItem(double amount, UUID target, String label) {
        ItemStack item = item(Material.GOLD_INGOT, label, List.of(
                amount <= 0 ? "&7Send the item as a gift." : "&7Buyer pays this amount from bank.")
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(PRICE_KEY, PersistentDataType.DOUBLE, amount);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack playerHead(Player owner, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(Util.color("&e" + owner.getName()));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Util.color(line));
            }
            meta.setLore(coloredLore);
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private static ItemStack attachPage(ItemStack item, int page) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void addNavigation(Inventory menu, int currentPage, int totalPages) {
        ItemStack indicator = item(Material.NAME_TAG, "&ePage " + (currentPage + 1) + " of " + totalPages,
                List.of("&7Pick who to trade with."));
        menu.setItem(49, indicator);

        if (currentPage > 0) {
            ItemStack prev = attachPage(item(Material.ARROW, "&aPrevious Page", List.of("&7Earlier players.")), currentPage - 1);
            menu.setItem(45, prev);
        }

        if (currentPage + 1 < totalPages) {
            ItemStack next = attachPage(item(Material.ARROW, "&aNext Page", List.of("&7More players.")), currentPage + 1);
            menu.setItem(53, next);
        }
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Util.color(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
