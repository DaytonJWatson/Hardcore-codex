package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.FreezeManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class AdminGui {

    public static final String MAIN_TITLE = Util.color("&4&lAdmin &8| &7Control Panel");
    public static final String PLAYER_LIST_TITLE = Util.color("&4&lAdmin &8| &7Online Players");
    private static final String PLAYER_ACTION_TITLE = Util.color("&4&lAdmin &8| &7Manage &e%player%");
    private static final String DURATION_TITLE = Util.color("&4&lAdmin &8| &7Choose Duration");
    private static final String BANK_TITLE = Util.color("&4&lAdmin &8| &7Bank Tools");
    private static final String AUCTION_TITLE = Util.color("&4&lAdmin &8| &7Auction Tools");

    private static final org.bukkit.NamespacedKey TARGET_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_target");
    private static final org.bukkit.NamespacedKey ACTION_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_action");
    private static final org.bukkit.NamespacedKey PAGE_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_page");

    private AdminGui() {
    }

    public static void openMain(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MAIN_TITLE);

        ItemStack managePlayers = item(Material.PLAYER_HEAD, "&eManage Players",
                List.of("&7Browse online players and run actions.", "&8Also lets you search offline players."));

        ItemStack searchPlayer = item(Material.BOOK, "&bSearch Player",
                List.of("&7Type any player name to open their panel."));

        ItemStack adminList = item(Material.PAPER, "&fAdmin List",
                List.of("&7Show configured Hardcore admins."));

        ItemStack adminLog = item(Material.OAK_SIGN, "&6Recent Admin Log",
                List.of("&7View the latest recorded actions."));

        ItemStack clearChat = item(Material.LIGHT_GRAY_CONCRETE, "&cClear Chat",
                List.of("&7Wipe chat for all players."));

        ItemStack auction = item(Material.GOLD_BLOCK, "&eAuction Tools",
                List.of("&7List and cancel auction listings."));

        ItemStack bank = item(Material.EMERALD_BLOCK, "&aBank Tools",
                List.of("&7Check and adjust bank balances."));

        ItemStack addAdmin = item(Material.LIME_DYE, "&aAdd Admin",
                List.of("&7Promote a player to Hardcore admin."));

        ItemStack removeAdmin = item(Material.RED_DYE, "&cRemove Admin",
                List.of("&7Demote a Hardcore admin."));

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        menu.setItem(10, managePlayers);
        menu.setItem(11, searchPlayer);
        menu.setItem(12, adminList);
        menu.setItem(13, adminLog);
        menu.setItem(14, clearChat);
        menu.setItem(15, auction);
        menu.setItem(16, bank);
        menu.setItem(21, addAdmin);
        menu.setItem(23, removeAdmin);

        player.openInventory(menu);
    }

    public static void openPlayerList(Player viewer, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, PLAYER_LIST_TITLE + " " + (page + 1));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int start = page * 45;
        int end = Math.min(start + 45, sorted.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            Player target = sorted.get(i);
            ItemStack head = playerHead(target.getUniqueId(), target.getName());
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                head.setItemMeta(meta);
            }
            menu.setItem(slot++, head);
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to admin panel."));
        menu.setItem(45, back);

        if (start > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (end < sorted.size()) {
            ItemStack next = item(Material.ARROW, "&eNext Page", List.of("&7Go to page " + (page + 2)));
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
                next.setItemMeta(meta);
            }
            menu.setItem(50, next);
        }

        viewer.openInventory(menu);
    }

    public static void openPlayerActions(Player viewer, OfflinePlayer target) {
        String title = PLAYER_ACTION_TITLE.replace("%player%", target.getName() == null ? "Unknown" : target.getName());
        Inventory menu = Bukkit.createInventory(null, 54, title);

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        String uuid = target.getUniqueId().toString();

        ItemStack info = actionItem(Material.BOOK, "&bInfo", uuid, List.of("&7View detailed info."));
        ItemStack status = actionItem(Material.MAP, "&eStatus", uuid, List.of("&7View punishments and role."));
        ItemStack invsee = actionItem(Material.CHEST, "&fInventory", uuid, List.of("&7Open their inventory."));
        ItemStack endersee = actionItem(Material.ENDER_CHEST, "&dEnder Chest", uuid, List.of("&7Open ender chest."));
        ItemStack warn = actionItem(Material.YELLOW_WOOL, "&eWarn", uuid, List.of("&7Send a warning."));
        ItemStack kick = actionItem(Material.REDSTONE, "&cKick", uuid, List.of("&7Kick with a reason."));
        ItemStack mute = actionItem(Material.RED_DYE, "&cMute", uuid, List.of("&7Choose a mute duration."));
        ItemStack unmute = actionItem(Material.LIME_DYE, "&aUnmute", uuid, List.of("&7Lift active mute."));
        ItemStack ban = actionItem(Material.ANVIL, "&4Ban", uuid, List.of("&7Choose a ban duration."));
        ItemStack unban = actionItem(Material.IRON_DOOR, "&aUnban", uuid, List.of("&7Remove existing ban."));
        ItemStack freeze = actionItem(Material.PACKED_ICE, "&bFreeze", uuid, List.of("&7Prevent movement."));
        ItemStack unfreeze = actionItem(Material.SHEARS, "&aUnfreeze", uuid, List.of("&7Release movement lock."));
        ItemStack tpTo = actionItem(Material.ENDER_PEARL, "&bTeleport To", uuid, List.of("&7Teleport to player."));
        ItemStack tpHere = actionItem(Material.COMPASS, "&bTeleport Here", uuid, List.of("&7Pull them to you."));
        ItemStack heal = actionItem(Material.GOLDEN_APPLE, "&aHeal", uuid, List.of("&7Restore health."));
        ItemStack feed = actionItem(Material.COOKED_BEEF, "&aFeed", uuid, List.of("&7Restore hunger."));
        ItemStack bank = actionItem(Material.EMERALD, "&aBank Tools", uuid, List.of("&7Check and edit balance."));
        ItemStack auctions = actionItem(Material.GOLD_INGOT, "&6Auctions", uuid,
                List.of("&7List their active auctions."));

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to player list."));

        menu.setItem(10, info);
        menu.setItem(11, status);
        menu.setItem(12, invsee);
        menu.setItem(13, endersee);
        menu.setItem(14, warn);
        menu.setItem(19, kick);
        menu.setItem(20, mute);
        if (MuteManager.isMuted(target.getUniqueId())) {
            menu.setItem(21, unmute);
        }
        menu.setItem(22, ban);
        if (BanManager.isBanned(target.getUniqueId())) {
            menu.setItem(23, unban);
        }
        menu.setItem(24, freeze);
        if (FreezeManager.isFrozen(target.getUniqueId())) {
            menu.setItem(25, unfreeze);
        }

        menu.setItem(28, tpTo);
        menu.setItem(29, tpHere);
        menu.setItem(30, heal);
        menu.setItem(31, feed);
        menu.setItem(32, bank);
        menu.setItem(33, auctions);

        menu.setItem(49, back);

        viewer.openInventory(menu);
    }

    public static void openDurationMenu(Player viewer, OfflinePlayer target, String actionLabel) {
        Inventory menu = Bukkit.createInventory(null, 27, DURATION_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack tenMinutes = durationItem(Material.CLOCK, "10m", target);
        ItemStack hour = durationItem(Material.COMPASS, "1h", target);
        ItemStack day = durationItem(Material.SPYGLASS, "1d", target);
        ItemStack permanent = durationItem(Material.OBSIDIAN, "permanent", target);
        ItemStack custom = durationItem(Material.WRITABLE_BOOK, "custom", target);

        menu.setItem(10, tenMinutes);
        menu.setItem(11, hour);
        menu.setItem(12, day);
        menu.setItem(14, permanent);
        menu.setItem(15, custom);

        ItemStack back = actionItem(Material.BARRIER, "&cBack", target.getUniqueId().toString(), List.of("&7Return."));
        menu.setItem(18, back);

        viewer.openInventory(menu);
    }

    public static void openBankMenu(Player viewer, OfflinePlayer target) {
        Inventory menu = Bukkit.createInventory(null, 27, BANK_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        String uuid = target.getUniqueId().toString();
        ItemStack balance = actionItem(Material.PAPER, "&fView Balance", uuid,
                List.of("&7Check current balance."));
        ItemStack deposit = actionItem(Material.EMERALD, "&aDeposit", uuid, List.of("&7Add funds."));
        ItemStack withdraw = actionItem(Material.REDSTONE, "&cWithdraw", uuid, List.of("&7Remove funds."));
        ItemStack set = actionItem(Material.NAME_TAG, "&eSet Balance", uuid, List.of("&7Set to exact amount."));
        ItemStack back = actionItem(Material.BARRIER, "&cBack", uuid, List.of("&7Return to player."));

        menu.setItem(10, balance);
        menu.setItem(12, deposit);
        menu.setItem(14, withdraw);
        menu.setItem(16, set);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    public static void openAuctionMenu(Player viewer) {
        Inventory menu = Bukkit.createInventory(null, 27, AUCTION_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack listAll = item(Material.GOLD_INGOT, "&eList All", List.of("&7Show all active auctions."));
        ItemStack listPlayer = item(Material.PLAYER_HEAD, "&bList by Player",
                List.of("&7Type a player name to filter."));
        ItemStack cancel = item(Material.BARRIER, "&cCancel Listing",
                List.of("&7Enter a listing id and optional reason."));
        ItemStack back = item(Material.ARROW, "&cBack", List.of("&7Return to admin panel."));

        menu.setItem(10, listAll);
        menu.setItem(12, listPlayer);
        menu.setItem(14, cancel);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    private static ItemStack durationItem(Material material, String key, OfflinePlayer target) {
        ItemStack item = item(material, "&e" + key.toUpperCase(), List.of("&7Select duration."));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack actionItem(Material material, String name, String targetUuid, List<String> lore) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, targetUuid);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack playerHead(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            skull.setDisplayName(Util.color("&e" + name));
            skull.setLore(List.of(Util.color("&7Click to manage.")));
            head.setItemMeta(skull);
        }
        return head;
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
