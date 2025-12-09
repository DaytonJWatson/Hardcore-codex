package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class BankSellGui {

    public static final String SELL_TITLE = Util.color("&6&lBank &8| &7Sell Items");

    private BankSellGui() {}

    public static void open(Player player) {
        BankManager bank = BankManager.get();
        Inventory menu = Bukkit.createInventory(null, 27, SELL_TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        double handValue = calculateSellPrice(player.getInventory().getItemInMainHand());
        ItemStack handItem = item(Material.HOPPER, "&aSell Item In Hand", List.of(
                "&7Item: &f" + formatStackName(player.getInventory().getItemInMainHand()),
                "&7Payout: &f" + bank.formatCurrency(handValue),
                "&8Includes durability and enchantment adjustments.",
                "&8Click to sell only what you're holding."
        ));

        double inventoryValue = calculateInventoryValue(player);
        ItemStack inventoryItem = item(Material.CHEST, "&aSell Entire Inventory", List.of(
                "&7Sells every item in your inventory.",
                "&7Payout: &f" + bank.formatCurrency(inventoryValue),
                "&8Armor, offhand, and hotbar are included.",
                "&cMake sure you really want to liquidate everything!"
        ));

        ItemStack info = item(Material.PAPER, "&bHow prices are calculated", List.of(
                "&7Values scale with rarity, stack size, and condition.",
                "&7Damaged items pay less; rare items pay more.",
                "&7Everything has a value but payouts are capped",
                "&7to prevent balance exploits."
        ));

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to account overview"));

        menu.setItem(10, handItem);
        menu.setItem(13, inventoryItem);
        menu.setItem(16, info);
        menu.setItem(22, back);

        player.openInventory(menu);
    }

    public static double calculateInventoryValue(Player player) {
        double total = 0.0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            total += calculateSellPrice(stack);
        }
        for (ItemStack stack : player.getInventory().getArmorContents()) {
            total += calculateSellPrice(stack);
        }
        total += calculateSellPrice(player.getInventory().getItemInOffHand());
        return roundCurrency(total);
    }

    public static double calculateSellPrice(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return 0.0;
        }

        Material type = stack.getType();
        int maxStack = Math.max(1, type.getMaxStackSize());
        double base = 0.05 + (64.0 / maxStack) * 0.15;

        String name = type.name();
        double rarityMultiplier = determineRarityMultiplier(name);

        double durabilityScale = 1.0;
        if (type.getMaxDurability() > 0) {
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof Damageable damageable) {
                double maxDurability = type.getMaxDurability();
                double remaining = Math.max(0, maxDurability - damageable.getDamage());
                durabilityScale = Math.max(0.05, remaining / maxDurability);
            }
        }

        double enchantBonus = 1.0 + (stack.getEnchantments().size() * 0.05);
        double price = stack.getAmount() * base * rarityMultiplier * durabilityScale * enchantBonus;

        double capped = Math.min(price, 5000.0);
        return roundCurrency(capped);
    }

    private static double determineRarityMultiplier(String name) {
        name = name.toUpperCase();
        if (name.contains("NETHERITE") || name.contains("ANCIENT_DEBRIS")) return 18.0;
        if (name.contains("DIAMOND")) return 12.0;
        if (name.contains("EMERALD")) return 10.0;
        if (name.contains("TOTEM")) return 14.0;
        if (name.contains("ELYTRA") || name.contains("SHULKER") || name.contains("SPAWN_EGG")) return 9.0;
        if (name.contains("GOLD")) return 6.0;
        if (name.contains("IRON")) return 4.0;
        if (name.contains("COPPER")) return 3.0;
        if (name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("QUARTZ") || name.contains("AMETHYST")) return 2.5;
        if (name.contains("BOOK")) return 2.0;
        if (name.contains("LEATHER") || name.contains("WOOL") || name.contains("GLASS")) return 1.5;
        return 1.0;
    }

    private static double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
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
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatStackName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return "Nothing";
        }

        ItemMeta meta = stack.getItemMeta();
        String displayName = meta != null ? meta.getDisplayName() : null;
        if (displayName != null && !ChatColor.stripColor(displayName).isEmpty()) {
            return ChatColor.stripColor(displayName) + " x" + stack.getAmount();
        }
        return Util.formatMaterialName(stack.getType()) + " x" + stack.getAmount();
    }
}
