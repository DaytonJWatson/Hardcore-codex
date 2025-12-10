package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.auction.AuctionListing;
import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.FreezeManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.managers.PlayerIpManager;
import com.daytonjwatson.hardcore.utils.Util;

import net.md_5.bungee.api.ChatColor;

public class AdminGui {

    public static final String TITLE = Util.color("&4Admin Control Panel");
    public static final String TITLE_PLAYER_PICKER = Util.color("&8Select a Player");
    public static final String TITLE_DURATION = Util.color("&8Select Duration");
    public static final String TITLE_BANK = Util.color("&8Bank Tools");
    public static final String TITLE_AUCTION = Util.color("&8Auction Tools");
    private static final NamespacedKey ACTION_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_action");
    private static final NamespacedKey TARGET_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_target");
    private static final NamespacedKey DATA_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_data");
    private static final NamespacedKey COMMAND_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_gui_cmd");

    private AdminGui() {
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, TITLE);
        inv.setItem(10, actionItem(Material.REDSTONE_BLOCK, ChatColor.DARK_RED + "Ban", "select_ban",
                "&fBan a player with a preset duration."));
        inv.setItem(11, actionItem(Material.LIME_DYE, ChatColor.GREEN + "Unban", "select_unban",
                "&fLift a ban from a player."));
        inv.setItem(12, actionItem(Material.MUSIC_DISC_13, ChatColor.RED + "Mute", "select_mute",
                "&fMute a player."));
        inv.setItem(13, actionItem(Material.FEATHER, ChatColor.GREEN + "Unmute", "select_unmute",
                "&fRemove a player's mute."));
        inv.setItem(14, actionItem(Material.PACKED_ICE, ChatColor.AQUA + "Freeze", "select_freeze",
                "&fFreeze an online player."));
        inv.setItem(15, actionItem(Material.SHEARS, ChatColor.GRAY + "Unfreeze", "select_unfreeze",
                "&fRelease a frozen player."));
        inv.setItem(16, actionItem(Material.PAPER, ChatColor.GOLD + "Warn", "select_warn",
                "&fSend a quick warning."));
        inv.setItem(19, actionItem(Material.STICK, ChatColor.RED + "Kick", "select_kick",
                "&fKick an online player."));
        inv.setItem(20, actionItem(Material.WHITE_WOOL, ChatColor.WHITE + "Clear Chat", "clear_chat",
                "&fClear global chat."));
        inv.setItem(21, actionItem(Material.BOOK, ChatColor.GOLD + "Help", "open_help",
                "&fOpen the text help panel."));
        inv.setItem(22, actionItem(Material.OAK_SIGN, ChatColor.AQUA + "Admin Logs", "show_logs",
                "&fView recent admin actions."));
        inv.setItem(23, actionItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "Status", "select_status",
                "&fInspect a player's flags."));
        inv.setItem(24, actionItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "Info", "select_info",
                "&fInspect player info."));
        inv.setItem(25, actionItem(Material.CHEST, ChatColor.GOLD + "Inventory", "select_invsee",
                "&fOpen a player's inventory."));
        inv.setItem(28, actionItem(Material.ENDER_CHEST, ChatColor.DARK_PURPLE + "Ender Chest", "select_endersee",
                "&fOpen a player's ender chest."));
        inv.setItem(29, actionItem(Material.ENDER_PEARL, ChatColor.BLUE + "Teleport", "select_tp",
                "&fTeleport to a player."));
        inv.setItem(30, actionItem(Material.LEAD, ChatColor.BLUE + "Summon", "select_tphere",
                "&fTeleport a player to you."));
        inv.setItem(31, actionItem(Material.GOLDEN_APPLE, ChatColor.GOLD + "Heal", "select_heal",
                "&fRestore a player's health."));
        inv.setItem(32, actionItem(Material.COOKED_BEEF, ChatColor.GREEN + "Feed", "select_feed",
                "&fRestore a player's hunger."));
        inv.setItem(33, actionItem(Material.GOLD_INGOT, ChatColor.GOLD + "Bank Tools", "select_bank",
                "&fManage a player's balance."));
        inv.setItem(34, actionItem(Material.EMERALD, ChatColor.GREEN + "Auction Tools", "open_auction",
                "&fReview and cancel listings."));
        inv.setItem(37, actionItem(Material.NAME_TAG, ChatColor.YELLOW + "Add Admin", "select_add_admin",
                "&fAdd a player to the admin list."));
        inv.setItem(38, actionItem(Material.BARRIER, ChatColor.RED + "Remove Admin", "select_remove_admin",
                "&fRemove a player from the admin list."));
        inv.setItem(39, actionItem(Material.PLAYER_HEAD, ChatColor.AQUA + "List Admins", "list_admins",
                "&fShow all configured admins."));
        inv.setItem(40, actionItem(Material.COMPASS, ChatColor.GRAY + "Refresh", "open_main",
                "&fRefresh this panel."));

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

    public static String getAction(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }

    public static UUID getTarget(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String id = meta.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String getData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(DATA_KEY, PersistentDataType.STRING);
    }

    public static ItemStack actionItem(Material icon, String name, String action, String... lore) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(name));
            meta.setLore(Arrays.stream(lore).map(Util::color).toList());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack commandItem(Material icon, String name, String command, String... lore) {
        ItemStack item = actionItem(icon, name, "execute_command", lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, command);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Inventory playerPicker(Player opener, String actionKey, List<OfflinePlayer> players, String emptyLore) {
        Inventory inv = Bukkit.createInventory(opener, 54, TITLE_PLAYER_PICKER);
        if (players.isEmpty()) {
            inv.setItem(22, actionItem(Material.BARRIER, ChatColor.RED + "No players", "open_main", emptyLore));
            return inv;
        }

        int slot = 0;
        for (OfflinePlayer target : players) {
            if (slot >= inv.getSize()) {
                break;
            }
            ItemStack head = playerHead(target, actionKey);
            inv.setItem(slot++, head);
        }
        return inv;
    }

    public static Inventory durationPicker(Player opener, UUID target, String targetName, String actionKey) {
        Inventory inv = Bukkit.createInventory(opener, 27, TITLE_DURATION);
        inv.setItem(10, durationItem(Material.CLOCK, "10m", target, targetName, actionKey));
        inv.setItem(11, durationItem(Material.CLOCK, "30m", target, targetName, actionKey));
        inv.setItem(12, durationItem(Material.CLOCK, "1h", target, targetName, actionKey));
        inv.setItem(13, durationItem(Material.CLOCK, "12h", target, targetName, actionKey));
        inv.setItem(14, durationItem(Material.CLOCK, "1d", target, targetName, actionKey));
        inv.setItem(15, durationItem(Material.CLOCK, "7d", target, targetName, actionKey));
        inv.setItem(16, durationItem(Material.NETHER_STAR, "Perm", target, targetName, actionKey));
        return inv;
    }

    public static Inventory bankPicker(Player opener, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(opener, 45, TITLE_BANK);
        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? uuid.toString() : target.getName();
        inv.setItem(4, bankActionItem(Material.GOLD_NUGGET, "Balance", uuid, name, "bank_balance"));

        int[] amounts = new int[] { 100, 500, 1000, 5000, 10000 };
        for (int i = 0; i < amounts.length; i++) {
            inv.setItem(19 + i, bankAmountItem(Material.GOLD_INGOT, "Deposit", uuid, name, "bank_deposit",
                    amounts[i]));
            inv.setItem(28 + i, bankAmountItem(Material.REDSTONE, "Withdraw", uuid, name, "bank_withdraw",
                    amounts[i]));
            inv.setItem(37 + i, bankAmountItem(Material.DIAMOND, "Set", uuid, name, "bank_set", amounts[i]));
        }
        return inv;
    }

    public static Inventory auctionPanel(Player opener) {
        Inventory inv = Bukkit.createInventory(opener, 54, TITLE_AUCTION);
        AuctionHouseManager manager = AuctionHouseManager.get();
        if (manager == null) {
            inv.setItem(22, actionItem(Material.BARRIER, ChatColor.RED + "Auction house offline", "open_main",
                    "&fAuction house not initialized."));
            return inv;
        }

        List<AuctionListing> listings = manager.getListings();
        if (listings.isEmpty()) {
            inv.setItem(22, actionItem(Material.BARRIER, ChatColor.YELLOW + "No active listings", "open_main",
                    "&fThere are no active auctions."));
            return inv;
        }

        int slot = 0;
        for (AuctionListing listing : listings) {
            if (slot >= inv.getSize()) {
                break;
            }

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String seller = listing.getSeller() == null ? "Server"
                        : Bukkit.getOfflinePlayer(listing.getSeller()).getName();
                meta.setDisplayName(Util.color(ChatColor.GOLD + "Listing " + listing.getId()));
                meta.setLore(Arrays.asList(Util.color("&fSeller: &e" + (seller == null ? "Unknown" : seller)),
                        Util.color("&fQty: &e" + listing.getQuantity()),
                        Util.color("&fPrice: &e" + listing.getPricePerItem()),
                        Util.color("&7Click to cancel")));
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "auction_cancel");
                meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, listing.getId().toString());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        inv.setItem(inv.getSize() - 1, actionItem(Material.MAP, ChatColor.GREEN + "List in chat", "auction_list",
                "&fSend current listings to chat."));
        return inv;
    }

    private static ItemStack playerHead(OfflinePlayer target, String actionKey) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = item.getItemMeta();
        if (!(baseMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(target);
        String name = target.getName() == null ? "Unknown" : target.getName();
        meta.setDisplayName(Util.color(ChatColor.YELLOW + name));
        meta.setLore(List.of(Util.color("&7Click to select")));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionKey);
        meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
        meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack durationItem(Material icon, String label, UUID target, String targetName, String actionKey) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(ChatColor.GOLD + label));
            meta.setLore(List.of(Util.color("&fTarget: &e" + targetName), Util.color("&7Click to apply")));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionKey);
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, label);
            meta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, targetName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack bankActionItem(Material icon, String label, UUID target, String targetName, String actionKey) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(ChatColor.GOLD + label));
            meta.setLore(List.of(Util.color("&fTarget: &e" + targetName), Util.color("&7Click to manage")));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionKey);
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, targetName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack bankAmountItem(Material icon, String label, UUID target, String targetName, String actionKey,
            int amount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(ChatColor.GREEN + label + " $" + amount));
            meta.setLore(Arrays.asList(Util.color("&fTarget: &e" + targetName),
                    Util.color("&7Click to apply $" + amount)));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionKey);
            meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, target.toString());
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, targetName);
            meta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, String.valueOf(amount));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<OfflinePlayer> gatherOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    public static List<OfflinePlayer> gatherBannedPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (String name : BanManager.getBannedNames()) {
            players.add(Bukkit.getOfflinePlayer(name));
        }
        return players;
    }

    public static List<OfflinePlayer> gatherMutedPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (String name : MuteManager.getMutedNames()) {
            players.add(Bukkit.getOfflinePlayer(name));
        }
        return players;
    }

    public static List<OfflinePlayer> gatherFrozenPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (String name : FreezeManager.getFrozenNames()) {
            players.add(Bukkit.getOfflinePlayer(name));
        }
        return players;
    }

    public static List<OfflinePlayer> gatherKnownPlayers() {
        Set<UUID> seen = new HashSet<>();
        List<OfflinePlayer> result = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (seen.add(online.getUniqueId())) {
                result.add(online);
            }
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (seen.add(offline.getUniqueId())) {
                result.add(offline);
            }
        }

        for (String name : PlayerIpManager.getStoredNames()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (seen.add(offline.getUniqueId())) {
                result.add(offline);
            }
        }
        return result;
    }

    public static List<OfflinePlayer> gatherAdmins() {
        List<OfflinePlayer> admins = new ArrayList<>();
        for (String name : AdminManager.getAdminNames()) {
            admins.add(Bukkit.getOfflinePlayer(name));
        }
        return admins;
    }
}
