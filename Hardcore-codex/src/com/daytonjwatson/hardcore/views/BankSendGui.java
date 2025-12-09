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

    private BankSendGui() {}

    public static void openRecipientSelection(Player player) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        online.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int size = Math.min(54, Math.max(27, ((online.size() / 9) + 1) * 9));
        Inventory menu = Bukkit.createInventory(null, size, SELECT_TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < size; i++) {
            menu.setItem(i, filler);
        }

        int slot = 10;
        int rowWidth = 7;
        for (Player target : online) {
            if (slot >= size - 9) break;
            ItemStack head = playerHead(target, List.of(
                    "&7Click to send money via GUI.",
                    "&8You'll choose an amount next."
            ));
            menu.setItem(slot, head);

            slot++;
            if ((slot - 1) % rowWidth == 0) {
                slot += 2; // skip edge columns to keep padding
            }
        }

        if (online.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&cNo players online", List.of(
                    "&7Invite a friend or use chat to send offline.")
            );
            menu.setItem(size / 2, empty);
        }

        ItemStack chatOption = item(Material.BOOK, "&ePrefer typing?", List.of(
                "&7Use &f/bank send <player> <amount> &7to send via chat.",
                "&8This is optional — the GUI works too."
        ));
        menu.setItem(size - 5, chatOption);

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to bank menu"));
        menu.setItem(size - 1, back);

        player.openInventory(menu);
    }

    public static void openAmountSelection(Player player, OfflinePlayer target) {
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
            ItemStack option = amountItem(amounts[i], target.getUniqueId());
            menu.setItem(slots[i], option);
        }

        ItemStack halfBalance = specialAmountItem("Half Balance", target.getUniqueId(), 0.5);
        ItemStack quarterBalance = specialAmountItem("Quarter Balance", target.getUniqueId(), 0.25);
        menu.setItem(3, halfBalance);
        menu.setItem(5, quarterBalance);

        ItemStack custom = item(Material.NAME_TAG, "&eEnter Custom Amount", List.of(
                "&7Click to type an amount in chat.",
                "&7Usage: &f/bank send " + targetName + " <amount>",
                "&8Chat entry stays available as an option."
        ));
        menu.setItem(22, custom);

        ItemStack back = item(Material.ARROW, "&7Choose different player", List.of("&7Go back to player list"));
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
