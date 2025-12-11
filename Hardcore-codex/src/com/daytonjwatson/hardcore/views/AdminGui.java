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
import com.daytonjwatson.hardcore.managers.AdminLogManager;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.FreezeManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.managers.ShopManager;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.utils.Util;

public final class AdminGui {

    public static final String MAIN_TITLE = Util.color("&4&lAdmin &8| &7Control Panel");
    public static final String PLAYER_LIST_TITLE = Util.color("&4&lAdmin &8| &7Online Players");
    private static final String PLAYER_ACTION_TITLE = Util.color("&4&lAdmin &8| &7Manage &e%player%");
    private static final String DURATION_TITLE = Util.color("&4&lAdmin &8| &7Choose Duration");
    private static final String REASON_TITLE = Util.color("&4&lAdmin &8| &7Choose Reason");
    private static final String AMOUNT_TITLE = Util.color("&4&lAdmin &8| &7Choose Amount");
    private static final String BANK_TITLE = Util.color("&4&lAdmin &8| &7Bank Tools");
    private static final String BANK_PLAYER_TITLE = Util.color("&4&lAdmin &8| &7Bank &ePlayer");
    private static final String BANK_TRANSACTIONS_TITLE = Util.color("&4&lAdmin &8| &7Bank &eHistory");
    private static final String AUCTION_TITLE = Util.color("&4&lAdmin &8| &7Auction Tools");
    private static final String AUCTION_SELLERS_TITLE = Util.color("&4&lAdmin &8| &7Auction Sellers");
    private static final String AUCTION_LISTINGS_TITLE = Util.color("&4&lAdmin &8| &7Auction Listings");
    public static final String SHOP_TITLE = Util.color("&4&lAdmin &8| &7Shop Tools");
    public static final String SHOP_OWNER_TITLE = Util.color("&4&lAdmin &8| &7Shop Owners");
    public static final String SHOP_LIST_TITLE = Util.color("&4&lAdmin &8| &7Shop Listings");
    public static final String SHOP_ACTION_TITLE = Util.color("&4&lAdmin &8| &7Manage Shop &e%shop%");
    public static final String LOG_TITLE = Util.color("&4&lAdmin &8| &7Admin Log");
    private static final int LOG_PAGE_SIZE = 45;

    private static final org.bukkit.NamespacedKey TARGET_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_target");
    private static final org.bukkit.NamespacedKey ACTION_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_action");
    private static final org.bukkit.NamespacedKey DURATION_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_duration");
    private static final org.bukkit.NamespacedKey PAGE_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_page");
    private static final org.bukkit.NamespacedKey FILTER_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_filter");
    private static final org.bukkit.NamespacedKey STATUS_FILTER_KEY = new org.bukkit.NamespacedKey(
            HardcorePlugin.getInstance(), "admin_status_filter");
    private static final org.bukkit.NamespacedKey LISTING_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_listing_id");
    private static final org.bukkit.NamespacedKey SHOP_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_shop_id");
    private static final org.bukkit.NamespacedKey SHOP_ACTION_KEY = new org.bukkit.NamespacedKey(
            HardcorePlugin.getInstance(), "admin_shop_action");

    private AdminGui() {
    }

    public static void openMain(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MAIN_TITLE);

        ItemStack managePlayers = item(Material.PLAYER_HEAD, "&eManage Players",
                List.of("&7Browse online players and run actions.", "&8Also lets you search offline players."));

        ItemStack broadcast = item(Material.NOTE_BLOCK, "&dBroadcast Message",
                List.of("&7Send a server-wide announcement."));

        boolean inSpectator = player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
        ItemStack gamemode = item(Material.COMPASS, inSpectator ? "&bSwitch to Survival" : "&bSwitch to Spectator",
                List.of("&7Quickly swap your admin view.",
                        inSpectator ? "&8Current: Spectator" : "&8Current: Survival"));

        ItemStack adminLog = item(Material.OAK_SIGN, "&6Recent Admin Log",
                List.of("&7View the latest recorded actions."));

        ItemStack clearChat = item(Material.LIGHT_GRAY_CONCRETE, "&cClear Chat",
                List.of("&7Wipe chat for all players."));

        ItemStack auction = item(Material.GOLD_BLOCK, "&eAuction Tools",
                List.of("&7List and cancel auction listings."));

        ItemStack bank = item(Material.EMERALD_BLOCK, "&aBank Tools",
                List.of("&7Check balances, history, and anomalies."));

        ItemStack shops = item(Material.CHEST, "&6Shop Tools", List.of("&7Review and manage player shops."));

        ItemStack addAdmin = item(Material.LIME_DYE, "&aAdd Admin",
                List.of("&7Promote a player to Hardcore admin."));

        ItemStack removeAdmin = item(Material.RED_DYE, "&cRemove Admin",
                List.of("&7Demote a Hardcore admin."));

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        menu.setItem(10, managePlayers);
        menu.setItem(11, broadcast);
        menu.setItem(12, gamemode);
        menu.setItem(13, adminLog);
        menu.setItem(14, clearChat);
        menu.setItem(15, auction);
        menu.setItem(16, bank);
        menu.setItem(17, shops);
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
        openPlayerActions(viewer, target, 0);
    }

    public static void openPlayerActions(Player viewer, OfflinePlayer target, int page) {
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

        ItemStack back = actionItem(Material.BARRIER, "&cBack", uuid, List.of("&7Return to player list."));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
            back.setItemMeta(backMeta);
        }

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

    public static void openAdminLog(Player viewer, String actorFilter, Boolean allowedOnly, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, LOG_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int start = page * LOG_PAGE_SIZE;
        List<AdminLogManager.LogEntry> entries = AdminLogManager.getRecentEntries(LOG_PAGE_SIZE * (page + 1) + 1,
                actorFilter, allowedOnly);
        int end = Math.min(entries.size(), start + LOG_PAGE_SIZE);

        if (start >= entries.size()) {
            ItemStack empty = item(Material.BARRIER, "&eNo admin log entries", List.of("&7No activity to display."));
            menu.setItem(22, empty);
        } else {
            for (int slot = 0, idx = start; idx < end; slot++, idx++) {
                AdminLogManager.LogEntry entry = entries.get(idx);
                ItemStack logItem = new ItemStack(entry.allowed() ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta meta = logItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(Util.color("&e" + entry.actor() + " &8- &7" + entry.readableTime()));
                    meta.setLore(List.of(Util.color("&7Status: " + (entry.allowed() ? "&aALLOWED" : "&cDENIED")),
                            Util.color("&7Command:"), Util.color("&f" + entry.command())));
                    logItem.setItemMeta(meta);
                }
                menu.setItem(slot, logItem);
            }
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to admin panel."));
        menu.setItem(45, back);

        ItemStack filter = item(Material.HOPPER, "&bFilter: " + (actorFilter == null ? "&fAll" : "&e" + actorFilter),
                List.of("&7Click to change filter."));
        ItemMeta filterMeta = filter.getItemMeta();
        if (filterMeta != null) {
            if (actorFilter != null) {
                filterMeta.getPersistentDataContainer().set(FILTER_KEY, PersistentDataType.STRING, actorFilter);
            }
            if (allowedOnly != null) {
                filterMeta.getPersistentDataContainer().set(STATUS_FILTER_KEY, PersistentDataType.INTEGER,
                        allowedOnly ? 1 : -1);
            }
            filter.setItemMeta(filterMeta);
        }
        menu.setItem(46, filter);

        String statusName = allowedOnly == null ? "&fAllowed &7/ &cDenied" : allowedOnly ? "&aAllowed" : "&cDenied";
        ItemStack status = item(Material.REPEATER, "&bStatus: " + statusName, List.of("&7Toggle allowed/denied view."));
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            if (allowedOnly != null) {
                statusMeta.getPersistentDataContainer().set(STATUS_FILTER_KEY, PersistentDataType.INTEGER,
                        allowedOnly ? 1 : -1);
            }
            if (actorFilter != null) {
                statusMeta.getPersistentDataContainer().set(FILTER_KEY, PersistentDataType.STRING, actorFilter);
            }
            status.setItemMeta(statusMeta);
        }
        menu.setItem(47, status);

        if (page > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                if (actorFilter != null) {
                    meta.getPersistentDataContainer().set(FILTER_KEY, PersistentDataType.STRING, actorFilter);
                }
                if (allowedOnly != null) {
                    meta.getPersistentDataContainer().set(STATUS_FILTER_KEY, PersistentDataType.INTEGER,
                            allowedOnly ? 1 : -1);
                }
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (entries.size() > end) {
            ItemStack next = item(Material.ARROW, "&eNext Page", List.of("&7Go to page " + (page + 2)));
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
                if (actorFilter != null) {
                    meta.getPersistentDataContainer().set(FILTER_KEY, PersistentDataType.STRING, actorFilter);
                }
                if (allowedOnly != null) {
                    meta.getPersistentDataContainer().set(STATUS_FILTER_KEY, PersistentDataType.INTEGER,
                            allowedOnly ? 1 : -1);
                }
                next.setItemMeta(meta);
            }
            menu.setItem(50, next);
        }

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

    public static void openBankPlayerList(Player viewer, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, BANK_PLAYER_TITLE + " " + (page + 1));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int start = page * 45;
        int end = Math.min(start + 45, players.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            Player target = players.get(i);
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

        ItemStack search = item(Material.COMPASS, "&bSearch Player",
                List.of("&7Search any player by name.", "&8Useful for offline lookups."));
        menu.setItem(49, search);

        if (start > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (end < players.size()) {
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

    public static void openBankMenu(Player viewer, OfflinePlayer target) {
        openBankMenu(viewer, target, null);
    }

    public static void openBankMenu(Player viewer, OfflinePlayer target, Integer returnPage) {
        Inventory menu = Bukkit.createInventory(null, 27, BANK_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        String uuid = target.getUniqueId().toString();
        double balanceAmount = com.daytonjwatson.hardcore.managers.BankManager.get().getBalance(target.getUniqueId());
        ItemStack balance = actionItem(Material.PAPER, "&fView Balance", uuid,
                List.of("&7Check current balance.", "&8" + Util.color("&fCurrent: &a"
                        + com.daytonjwatson.hardcore.managers.BankManager.get().formatCurrency(balanceAmount))));
        ItemStack deposit = actionItem(Material.EMERALD, "&aDeposit", uuid, List.of("&7Add funds via preset amounts."));
        ItemStack withdraw = actionItem(Material.REDSTONE, "&cWithdraw", uuid,
                List.of("&7Remove funds via preset amounts."));
        ItemStack set = actionItem(Material.NAME_TAG, "&eSet Balance", uuid, List.of("&7Set to amount via presets."));
        ItemStack transactions = actionItem(Material.BOOK, "&bRecent Transactions", uuid,
                List.of("&7Open a log of the latest entries.", "&8Spot suspicious spikes quickly."));
        ItemStack anomalies = actionItem(Material.SCULK_SENSOR, "&cSuspicious Activity", uuid,
                List.of("&7Show quick net-change summary.", "&8Look for duping or spikes."));
        ItemStack back = actionItem(Material.BARRIER, "&cBack", uuid, List.of("&7Return to player."));
        if (returnPage != null) {
            ItemMeta backMeta = back.getItemMeta();
            if (backMeta != null) {
                backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, returnPage);
                back.setItemMeta(backMeta);
            }
        }

        menu.setItem(10, balance);
        menu.setItem(12, deposit);
        menu.setItem(14, withdraw);
        menu.setItem(16, set);
        menu.setItem(20, transactions);
        menu.setItem(22, anomalies);
        menu.setItem(24, back);

        viewer.openInventory(menu);
    }

    public static void openAuctionMenu(Player viewer) {
        Inventory menu = Bukkit.createInventory(null, 27, AUCTION_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack listAll = item(Material.GOLD_INGOT, "&eView All Listings", List.of("&7Browse every active listing."));
        ItemStack listPlayer = item(Material.PLAYER_HEAD, "&bFilter by Seller",
                List.of("&7Pick a seller head to filter."));
        ItemStack commands = item(Material.OAK_SIGN, "&7Command Shortcuts",
                List.of("&7Use chat commands for power users.", "&8/ admin auction list|cancel"));
        ItemStack back = item(Material.ARROW, "&cBack", List.of("&7Return to admin panel."));

        menu.setItem(11, listAll);
        menu.setItem(13, listPlayer);
        menu.setItem(15, commands);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    public static void openShopMenu(Player viewer) {
        Inventory menu = Bukkit.createInventory(null, 27, SHOP_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack listAll = item(Material.CHEST, "&eView All Shops",
                List.of("&7Browse every player shop."));
        ItemStack listOwners = item(Material.PLAYER_HEAD, "&bFilter by Owner",
                List.of("&7Pick a shop owner to filter."));
        ItemStack browser = item(Material.EMERALD, "&aOpen Public Browser", List.of("&7Open the normal shop browser."));
        ItemStack back = item(Material.ARROW, "&cBack", List.of("&7Return to admin panel."));

        menu.setItem(11, listAll);
        menu.setItem(13, listOwners);
        menu.setItem(15, browser);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    public static void openAuctionSellerList(Player viewer, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, AUCTION_SELLERS_TITLE + " " + (page + 1));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        java.util.Map<UUID, Long> counts = new java.util.HashMap<>();
        com.daytonjwatson.hardcore.managers.AuctionHouseManager.get().getListings()
                .forEach(listing -> counts.merge(listing.getSeller(), 1L, Long::sum));
        List<UUID> sellers = new ArrayList<>(counts.keySet());
        sellers.sort(java.util.Comparator.comparing(uuid -> Bukkit.getOfflinePlayer(uuid).getName(),
                java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        int start = page * 45;
        int end = Math.min(start + 45, sellers.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            UUID seller = sellers.get(i);
            String name = Bukkit.getOfflinePlayer(seller).getName();
            ItemStack head = playerHead(seller, name == null ? "Unknown" : name);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, seller.toString());
                meta.setLore(List.of(Util.color("&7Listings: &f" + counts.getOrDefault(seller, 0L)),
                        Util.color("&7Click to filter.")));
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                head.setItemMeta(meta);
            }
            menu.setItem(slot++, head);
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to auction tools."));
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

        if (end < sellers.size()) {
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

    public static void openShopOwnerList(Player viewer, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, SHOP_OWNER_TITLE + " " + (page + 1));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
        ShopManager.get().getShops().forEach(shop -> counts.merge(shop.getOwner(), 1, Integer::sum));
        List<UUID> owners = new ArrayList<>(counts.keySet());
        owners.sort(java.util.Comparator.comparing(uuid -> Bukkit.getOfflinePlayer(uuid).getName(),
                java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        int start = page * 45;
        int end = Math.min(start + 45, owners.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            UUID owner = owners.get(i);
            String name = Bukkit.getOfflinePlayer(owner).getName();
            ItemStack head = playerHead(owner, name == null ? "Unknown" : name);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, owner.toString());
                meta.setLore(List.of(Util.color("&7Shops: &f" + counts.getOrDefault(owner, 0)),
                        Util.color("&7Click to filter.")));
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                head.setItemMeta(meta);
            }
            menu.setItem(slot++, head);
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to shop tools."));
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

        if (end < owners.size()) {
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

    public static void openShopList(Player viewer, int page, UUID ownerFilter) {
        Inventory menu = Bukkit.createInventory(null, 54, SHOP_LIST_TITLE + " " + (page + 1));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        List<PlayerShop> shops = new ArrayList<>();
        for (PlayerShop shop : ShopManager.get().getShops()) {
            if (ownerFilter == null || ownerFilter.equals(shop.getOwner())) {
                shops.add(shop);
            }
        }
        shops.sort(java.util.Comparator.comparing(PlayerShop::getName, String.CASE_INSENSITIVE_ORDER));

        int start = page * 45;
        int end = Math.min(start + 45, shops.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            PlayerShop shop = shops.get(i);
            ItemStack icon = shop.getIcon();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Owner: &f" + Util.resolveShopOwnerName(shop)));
                lore.add(Util.color("&7Status: " + (shop.isOpen() ? "&aOpen" : "&cClosed")));
                lore.add(Util.color("&7Listings: &f" + shop.getStock().size() + "&7/27"));
                lore.add(Util.color("&7Notifications: " + (shop.isNotificationsEnabled() ? "&aOn" : "&cOff")));
                lore.add(Util.color("&8Click for admin actions."));
                meta.setLore(lore);
                meta.setDisplayName(Util.color(shop.getName()));
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                if (ownerFilter != null) {
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
                }
                icon.setItemMeta(meta);
            }
            menu.setItem(slot++, icon);
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to shop tools."));
        if (ownerFilter != null) {
            ItemMeta meta = back.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
                back.setItemMeta(meta);
            }
        }
        menu.setItem(45, back);

        if (start > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                if (ownerFilter != null) {
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
                }
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (end < shops.size()) {
            ItemStack next = item(Material.ARROW, "&eNext Page", List.of("&7Go to page " + (page + 2)));
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
                if (ownerFilter != null) {
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
                }
                next.setItemMeta(meta);
            }
            menu.setItem(50, next);
        }

        viewer.openInventory(menu);
    }

    public static void openShopActions(Player viewer, PlayerShop shop, Integer returnPage, UUID ownerFilter) {
        String title = SHOP_ACTION_TITLE.replace("%shop%", shop.getName() == null ? "Shop" : shop.getName());
        Inventory menu = Bukkit.createInventory(null, 27, title);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack view = shopAction(Material.MAP, "&bView Shopfront",
                List.of("&7Open the shop as a customer."), "view", shop, returnPage, ownerFilter);
        ItemStack edit = shopAction(Material.ANVIL, "&eEdit Shop",
                List.of("&7Open the shop editor."), "edit", shop, returnPage, ownerFilter);
        ItemStack toggle = shopAction(shop.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
                shop.isOpen() ? "&aOpen Shop" : "&cClosed Shop", List.of("&7Toggle buying access."), "toggle", shop,
                returnPage, ownerFilter);
        ItemStack notifications = shopAction(shop.isNotificationsEnabled() ? Material.BELL : Material.NOTE_BLOCK,
                shop.isNotificationsEnabled() ? "&aNotifications Enabled" : "&cNotifications Disabled",
                List.of("&7Toggle owner notifications."), "notifications", shop, returnPage, ownerFilter);
        ItemStack delete = shopAction(Material.REDSTONE_BLOCK, "&cDelete Shop",
                List.of("&7Remove this shop and", "&7return items to you."), "delete", shop, returnPage,
                ownerFilter);

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to shop list."));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (returnPage != null) {
                backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, returnPage);
            }
            backMeta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
            if (ownerFilter != null) {
                backMeta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
            }
            back.setItemMeta(backMeta);
        }

        menu.setItem(10, view);
        menu.setItem(11, edit);
        menu.setItem(13, toggle);
        menu.setItem(15, notifications);
        menu.setItem(16, delete);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    public static void openAuctionListings(Player viewer, UUID sellerFilter, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, AUCTION_LISTINGS_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        List<com.daytonjwatson.hardcore.auction.AuctionListing> listings = com.daytonjwatson.hardcore.managers.AuctionHouseManager
                .get().getListings();
        if (sellerFilter != null) {
            listings = listings.stream().filter(l -> sellerFilter.equals(l.getSeller())).toList();
        }

        listings.sort(java.util.Comparator.comparing(com.daytonjwatson.hardcore.auction.AuctionListing::getExpiresAt));
        int start = page * 45;
        int end = Math.min(start + 45, listings.size());
        int slot = 0;
        if (listings.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&eNo active listings", List.of("&7Nothing to manage."));
            menu.setItem(22, empty);
        } else {
            for (int i = start; i < end; i++) {
                com.daytonjwatson.hardcore.auction.AuctionListing listing = listings.get(i);
                ItemStack item = listing.getItem().clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String sellerName = listing.getSeller() == null ? "Server"
                            : Bukkit.getOfflinePlayer(listing.getSeller()).getName();
                    List<String> lore = new ArrayList<>();
                    lore.add(Util.color("&7ID: &f" + listing.getId()));
                    lore.add(Util.color("&7Seller: &f" + (sellerName == null ? "Unknown" : sellerName)));
                    lore.add(Util.color("&7Qty: &f" + listing.getQuantity()));
                    lore.add(Util.color("&7Price: &f"
                            + com.daytonjwatson.hardcore.managers.BankManager.get().formatCurrency(listing.getPricePerItem())));
                    lore.add(Util.color("&7Expires: &f"
                            + java.time.Instant.ofEpochMilli(listing.getExpiresAt()).toString()));
                    lore.add(Util.color("&cClick to cancel listing."));
                    meta.setLore(lore);
                    meta.getPersistentDataContainer().set(LISTING_KEY, PersistentDataType.STRING,
                            listing.getId().toString());
                    if (sellerFilter != null) {
                        meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, sellerFilter.toString());
                    }
                    meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                    item.setItemMeta(meta);
                }
                menu.setItem(slot++, item);
            }
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to auction filters."));
        if (sellerFilter != null) {
            ItemMeta backMeta = back.getItemMeta();
            if (backMeta != null) {
                backMeta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, sellerFilter.toString());
                backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                back.setItemMeta(backMeta);
            }
        }
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
            back.setItemMeta(backMeta);
        }
        menu.setItem(45, back);

        if (page > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                if (sellerFilter != null) {
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, sellerFilter.toString());
                }
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (end < listings.size()) {
            ItemStack next = item(Material.ARROW, "&eNext Page", List.of("&7Go to page " + (page + 2)));
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
                if (sellerFilter != null) {
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, sellerFilter.toString());
                }
                next.setItemMeta(meta);
            }
            menu.setItem(50, next);
        }

        viewer.openInventory(menu);
    }

    public static void openBankTransactions(Player viewer, OfflinePlayer target, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, BANK_TRANSACTIONS_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        List<String> transactions = com.daytonjwatson.hardcore.managers.BankManager.get()
                .getTransactions(target.getUniqueId());
        int start = page * 45;
        int end = Math.min(start + 45, transactions.size());
        int slot = 0;
        if (transactions.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&eNo history found", List.of("&7This account has no log yet."));
            menu.setItem(22, empty);
        } else {
            for (int i = start; i < end; i++) {
                String entry = transactions.get(i);
                ItemStack book = new ItemStack(Material.PAPER);
                ItemMeta meta = book.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(Util.color("&eEntry #" + (i + 1)));
                    meta.setLore(List.of(Util.color("&7" + entry)));
                    meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING,
                            target.getUniqueId().toString());
                    meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
                    book.setItemMeta(meta);
                }
                menu.setItem(slot++, book);
            }
        }

        ItemStack back = actionItem(Material.BARRIER, "&cBack", target.getUniqueId().toString(), List.of("&7Return to bank."));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
            back.setItemMeta(backMeta);
        }
        menu.setItem(45, back);

        if (page > 0) {
            ItemStack prev = item(Material.ARROW, "&ePrevious Page", List.of("&7Go to page " + page));
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, Math.max(0, page - 1));
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
                prev.setItemMeta(meta);
            }
            menu.setItem(48, prev);
        }

        if (end < transactions.size()) {
            ItemStack next = item(Material.ARROW, "&eNext Page", List.of("&7Go to page " + (page + 2)));
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
                next.setItemMeta(meta);
            }
            menu.setItem(50, next);
        }

        viewer.openInventory(menu);
    }

    private static ItemStack durationItem(Material material, String key, OfflinePlayer target) {
        ItemStack item = item(material, "&e" + key.toUpperCase(), List.of("&7Select duration."));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
            meta.getPersistentDataContainer().set(DURATION_KEY, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openReasonMenu(Player viewer, OfflinePlayer target, String action, String durationKey) {
        Inventory menu = Bukkit.createInventory(null, 27, REASON_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack spam = reasonItem(Material.PAPER, "Spam/Advertising", target, action, durationKey,
                "Spamming or advertising");
        ItemStack harassment = reasonItem(Material.BOOK, "Harassment", target, action, durationKey, "Harassment");
        ItemStack cheating = reasonItem(Material.BARRIER, "Cheating", target, action, durationKey,
                "Cheating or unfair advantage");
        ItemStack griefing = reasonItem(Material.FLINT_AND_STEEL, "Griefing", target, action, durationKey,
                "Griefing");
        ItemStack other = reasonItem(Material.WRITABLE_BOOK, "Custom Reason", target, action, durationKey,
                "custom");
        ItemStack back = actionItem(Material.BARRIER, "&cBack", target.getUniqueId().toString(), List.of("&7Return."));

        menu.setItem(10, spam);
        menu.setItem(11, harassment);
        menu.setItem(12, cheating);
        menu.setItem(14, griefing);
        menu.setItem(15, other);
        menu.setItem(22, back);

        viewer.openInventory(menu);
    }

    public static void openAmountMenu(Player viewer, OfflinePlayer target, String action) {
        Inventory menu = Bukkit.createInventory(null, 27, AMOUNT_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        ItemStack amount10 = amountItem(Material.COAL, "10", target, action);
        ItemStack amount100 = amountItem(Material.IRON_INGOT, "100", target, action);
        ItemStack amount500 = amountItem(Material.GOLD_INGOT, "500", target, action);
        ItemStack amount1000 = amountItem(Material.DIAMOND, "1000", target, action);
        ItemStack custom = amountItem(Material.WRITABLE_BOOK, "Custom", target, action);
        ItemStack back = actionItem(Material.BARRIER, "&cBack", target.getUniqueId().toString(), List.of("&7Return."));

        menu.setItem(10, amount10);
        menu.setItem(11, amount100);
        menu.setItem(12, amount500);
        menu.setItem(14, amount1000);
        menu.setItem(15, custom);
        menu.setItem(22, back);

        viewer.openInventory(menu);
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

    private static ItemStack shopAction(Material material, String name, List<String> lore, String action, PlayerShop shop,
            Integer returnPage, UUID ownerFilter) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
            meta.getPersistentDataContainer().set(SHOP_ACTION_KEY, PersistentDataType.STRING, action);
            if (returnPage != null) {
                meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, returnPage);
            }
            if (ownerFilter != null) {
                meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, ownerFilter.toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack reasonItem(Material material, String name, OfflinePlayer target, String action,
            String duration, String reasonKey) {
        ItemStack item = actionItem(material, "&e" + name, target.getUniqueId().toString(),
                List.of("&7Click to confirm."));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            if (duration != null) {
                meta.getPersistentDataContainer().set(DURATION_KEY, PersistentDataType.STRING, duration);
            }
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(), "admin_reason"),
                    PersistentDataType.STRING, reasonKey);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack amountItem(Material material, String amountLabel, OfflinePlayer target, String action) {
        ItemStack item = actionItem(material, "&e" + amountLabel, target.getUniqueId().toString(),
                List.of("&7Adjust balance."));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            if (!"custom".equalsIgnoreCase(amountLabel)) {
                meta.getPersistentDataContainer().set(DURATION_KEY, PersistentDataType.STRING, amountLabel);
            }
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
