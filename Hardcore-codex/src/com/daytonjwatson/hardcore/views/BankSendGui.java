package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class BankSendGui {

    public static final String SELECT_TITLE = Util.color("&6&lBank &8| &7Choose Recipient");
    public static final String AMOUNT_TITLE_PREFIX = Util.color("&6&lBank &8| &7Send to ");

    private static final NamespacedKey TARGET_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_send_target");
    private static final NamespacedKey AMOUNT_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_send_amount");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bank_send_page");
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private BankSendGui() {}

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
                    "&7Click to send money via GUI.",
                    "&8You'll choose an amount next."
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
                    "&7Invite a friend or use chat to send offline.")
            );
            menu.setItem(22, empty);
        }

        ItemStack chatOption = item(Material.BOOK, "&ePrefer typing?", List.of(
                "&7Use &f/bank send <player> <amount> &7to send via chat.",
                "&8This is optional — the GUI works too."
        ));
        menu.setItem(50, chatOption);

        ItemStack back = attachPage(item(Material.BARRIER, "&cBack", List.of("&7Return to bank menu")), 0);
        menu.setItem(47, back);

        addNavigation(menu, currentPage, totalPages);

        player.openInventory(menu);
    }

    public static void openAmountSelection(Player player, OfflinePlayer target) {
        openAmountSelection(player, target, 0);
    }

    public static void openAmountSelection(Player player, OfflinePlayer target, int fromPage) {
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        String title = AMOUNT_TITLE_PREFIX + Util.color("&e" + targetName);

        Inventory menu = Bukkit.createInventory(null, 27, title);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        double[] amounts = {50, 100, 250, 500, 1000, 5000};
        int[] slots = {10, 11, 12, 14, 15, 16};
        for (int i = 0; i < amounts.length; i++) {
            ItemStack option = attachPage(amountItem(amounts[i], target.getUniqueId()), fromPage);
            menu.setItem(slots[i], option);
        }

        ItemStack halfBalance = attachPage(specialAmountItem("Half Balance", target.getUniqueId(), 0.5), fromPage);
        ItemStack quarterBalance = attachPage(specialAmountItem("Quarter Balance", target.getUniqueId(), 0.25), fromPage);
        menu.setItem(3, halfBalance);
        menu.setItem(5, quarterBalance);

        ItemStack custom = item(Material.NAME_TAG, "&eEnter Custom Amount", List.of(
                "&7Click to type just the amount in chat.",
                "&7Or type &ccancel &7to back out.",
                "&8Chat entry stays available as an option."
        ));
        ItemMeta customMeta = custom.getItemMeta();
        if (customMeta != null) {
            customMeta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
            customMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, fromPage);
            custom.setItemMeta(customMeta);
        }
        menu.setItem(22, custom);

        ItemStack back = attachPage(item(Material.ARROW, "&7Choose different player", List.of("&7Go back to player list")), fromPage);
        menu.setItem(18, back);

        player.openInventory(menu);
    }

    public static boolean isSendInventory(String title) {
        String stripped = org.bukkit.ChatColor.stripColor(title);
        return stripped.equals(org.bukkit.ChatColor.stripColor(SELECT_TITLE))
                || stripped.startsWith(org.bukkit.ChatColor.stripColor(AMOUNT_TITLE_PREFIX));
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

    public static Double getAmountFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        Double stored = item.getItemMeta().getPersistentDataContainer().get(AMOUNT_KEY, PersistentDataType.DOUBLE);
        if (stored != null) {
            return stored;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(AMOUNT_KEY, PersistentDataType.STRING);
        if (raw == null) return null;
        if (raw.startsWith("MULTIPLIER:")) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String getMultiplierData(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        if (!item.getItemMeta().getPersistentDataContainer().has(AMOUNT_KEY, PersistentDataType.STRING)) {
            return null;
        }

        String raw = item.getItemMeta().getPersistentDataContainer().get(AMOUNT_KEY, PersistentDataType.STRING);
        if (raw == null || !raw.startsWith("MULTIPLIER:")) {
            return null;
        }
        return raw.replace("MULTIPLIER:", "");
    }

    public static Integer getPageFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(PAGE_KEY, PersistentDataType.INTEGER);
    }

    public static ItemStack amountItem(double amount, UUID target) {
        ItemStack item = item(Material.GOLD_INGOT, "&a" + BankManager.get().formatCurrency(amount), List.of(
                "&7Send this exact amount."));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.DOUBLE, amount);
            item.setItemMeta(meta);
        }
        return item;
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
                List.of("&7Browse online players."));
        menu.setItem(49, indicator);

        if (currentPage > 0) {
            ItemStack prev = attachPage(item(Material.ARROW, "&aPrevious Page", List.of("&7Show earlier names.")), currentPage - 1);
            menu.setItem(45, prev);
        }

        if (currentPage + 1 < totalPages) {
            ItemStack next = attachPage(item(Material.ARROW, "&aNext Page", List.of("&7Show more names.")), currentPage + 1);
            menu.setItem(53, next);
        }
    }

    private static ItemStack specialAmountItem(String label, UUID target, double balanceMultiplier) {
        ItemStack item = item(Material.GOLD_BLOCK, "&eSend " + label, Arrays.asList(
                "&7Uses your current balance multiplier:",
                "&f" + (int) (balanceMultiplier * 100) + "%",
                "&8Click to send based on live balance."
        ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.STRING, "MULTIPLIER:" + balanceMultiplier);
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
