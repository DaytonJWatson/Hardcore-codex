package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.utils.Util;

import net.md_5.bungee.api.ChatColor;

public class AdminGui {

    public record CommandAction(Material icon, String name, String command, boolean executeDirect, String... lore) {
    }

    public static final String TITLE = Util.color("&4Admin Control Panel");
    private static final NamespacedKey COMMAND_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_cmd");
    private static final NamespacedKey EXECUTE_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_exec");

    private AdminGui() {
    }

    public static void open(Player player) {
        List<CommandAction> actions = actions();
        int size = Math.max(27, ((actions.size() / 9) + 1) * 9);
        Inventory inv = Bukkit.createInventory(player, Math.min(size, 54), TITLE);

        for (int i = 0; i < actions.size() && i < inv.getSize(); i++) {
            inv.setItem(i, build(actions.get(i)));
        }

        player.openInventory(inv);
    }

    public static String getCommand(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(COMMAND_KEY, PersistentDataType.STRING);
    }

    public static boolean shouldExecuteDirect(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer flag = container.get(EXECUTE_KEY, PersistentDataType.INTEGER);
        return flag != null && flag == 1;
    }

    private static ItemStack build(CommandAction action) {
        ItemStack item = new ItemStack(action.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(action.name()));
            List<String> lore = new ArrayList<>();
            lore.add(Util.color("&7" + action.command()));
            Arrays.stream(action.lore()).map(Util::color).forEach(lore::add);
            meta.setLore(lore);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(COMMAND_KEY, PersistentDataType.STRING, action.command());
            if (action.executeDirect()) {
                container.set(EXECUTE_KEY, PersistentDataType.INTEGER, 1);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<CommandAction> actions() {
        List<CommandAction> actions = new ArrayList<>();
        actions.add(new CommandAction(Material.BOOK, ChatColor.GOLD + "Help", "/admin help", true,
                "&fView the text-based admin help panel."));
        actions.add(new CommandAction(Material.NAME_TAG, ChatColor.YELLOW + "Add Admin", "/admin add ", false,
                "&fAdd a player to the admin list."));
        actions.add(new CommandAction(Material.BARRIER, ChatColor.RED + "Remove Admin", "/admin remove ", false,
                "&fRemove a player from the admin list."));
        actions.add(new CommandAction(Material.PLAYER_HEAD, ChatColor.AQUA + "List Admins", "/admin list", true,
                "&fShow all configured admins."));
        actions.add(new CommandAction(Material.REDSTONE_BLOCK, ChatColor.DARK_RED + "Ban", "/admin ban ", false,
                "&fBan a player with optional duration and reason."));
        actions.add(new CommandAction(Material.LIME_DYE, ChatColor.GREEN + "Unban", "/admin unban ", false,
                "&fUnban a player."));
        actions.add(new CommandAction(Material.STICK, ChatColor.RED + "Kick", "/admin kick ", false,
                "&fKick an online player."));
        actions.add(new CommandAction(Material.MUSIC_DISC_13, ChatColor.RED + "Mute", "/admin mute ", false,
                "&fMute a player with optional duration and reason."));
        actions.add(new CommandAction(Material.FEATHER, ChatColor.GREEN + "Unmute", "/admin unmute ", false,
                "&fLift a player's mute."));
        actions.add(new CommandAction(Material.PAPER, ChatColor.GOLD + "Warn", "/admin warn ", false,
                "&fSend a warning to an online player."));
        actions.add(new CommandAction(Material.PACKED_ICE, ChatColor.AQUA + "Freeze", "/admin freeze ", false,
                "&fFreeze an online player."));
        actions.add(new CommandAction(Material.SHEARS, ChatColor.GRAY + "Unfreeze", "/admin unfreeze ", false,
                "&fRelease a frozen player."));
        actions.add(new CommandAction(Material.WHITE_WOOL, ChatColor.WHITE + "Clear Chat", "/admin clearchat", true,
                "&fClear chat with the default reason."));
        actions.add(new CommandAction(Material.CLOCK, ChatColor.YELLOW + "Status", "/admin status ", false,
                "&fView a player's punishment status."));
        actions.add(new CommandAction(Material.WRITABLE_BOOK, ChatColor.GOLD + "Info", "/admin info ", false,
                "&fInspect player IPs, location, and status."));
        actions.add(new CommandAction(Material.CHEST, ChatColor.GOLD + "Invsee", "/admin invsee ", false,
                "&fOpen a player's inventory."));
        actions.add(new CommandAction(Material.ENDER_CHEST, ChatColor.DARK_PURPLE + "Endersee", "/admin endersee ",
                false, "&fOpen a player's ender chest."));
        actions.add(new CommandAction(Material.ENDER_PEARL, ChatColor.BLUE + "Teleport", "/admin tp ", false,
                "&fTeleport to a player or move one to another."));
        actions.add(new CommandAction(Material.LEAD, ChatColor.BLUE + "Teleport Here", "/admin tphere ", false,
                "&fPull a player to your location."));
        actions.add(new CommandAction(Material.GOLDEN_APPLE, ChatColor.GOLD + "Heal", "/admin heal ", false,
                "&fRestore a player's health."));
        actions.add(new CommandAction(Material.COOKED_BEEF, ChatColor.GREEN + "Feed", "/admin feed ", false,
                "&fRestore a player's hunger."));
        actions.add(new CommandAction(Material.OAK_SIGN, ChatColor.AQUA + "Logs", "/admin log ", false,
                "&fView recent admin actions."));
        actions.add(new CommandAction(Material.GOLD_INGOT, ChatColor.GOLD + "Bank Tools", "/admin bank ", false,
                "&fManage a player's bank balance."));
        actions.add(new CommandAction(Material.EMERALD, ChatColor.GREEN + "Auction Tools", "/admin auction ", false,
                "&fView or cancel auction listings."));
        return actions;
    }
}
