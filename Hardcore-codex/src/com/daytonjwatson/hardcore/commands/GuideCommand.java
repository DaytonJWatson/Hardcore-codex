package com.daytonjwatson.hardcore.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import org.bukkit.ChatColor;

import com.daytonjwatson.hardcore.utils.MessageStyler;

public class GuideCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        sendGuideChat(player);
        giveGuideBook(player);

        return true;
    }

    private void sendGuideChat(Player p) {

        MessageStyler.sendPanel(p, "Bandits & Heroes Guide",
                ChatColor.GOLD + "Bandits",
                "Unfair kills on weaker players give " + ChatColor.DARK_RED + "Bandit Points" + ChatColor.GRAY + ".",
                "At " + ChatColor.RED + "3 unfair kills" + ChatColor.GRAY + " you become a " + ChatColor.DARK_RED + ChatColor.BOLD + "Bandit" + ChatColor.GRAY + ".",
                "Bandits are announced and marked " + ChatColor.DARK_RED + "[B]" + ChatColor.GRAY + ".",
                ChatColor.GOLD + "Redemption",
                "Bandits who slay bandits gain " + ChatColor.GREEN + "Redemption Points" + ChatColor.GRAY + ".",
                "At " + ChatColor.GREEN + "3 bandits slain as a bandit" + ChatColor.GRAY + ", you lose Bandit status.",
                ChatColor.GOLD + "Heroes",
                "Non-bandits who kill bandits gain " + ChatColor.GOLD + "Hero progress" + ChatColor.GRAY + ".",
                "At " + ChatColor.GOLD + "3 bandits slain as a non-bandit" + ChatColor.GRAY + ", you become a " + ChatColor.GOLD + ChatColor.BOLD + "Hero" + ChatColor.GRAY + ".",
                "Heroes are marked " + ChatColor.GOLD + "[H]" + ChatColor.GRAY + " and announced.",
                "You can become a Hero after redeeming from Bandit.");
    }

    private void giveGuideBook(Player player) {

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        meta.setTitle(ChatColor.GOLD + "Bandits & Heroes");
        meta.setAuthor("HardcoreGuide");

        // Page 1: Bandits
        String page1 =
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "BANDITS\n\n" +
                ChatColor.DARK_GRAY +
                "Killing much\n" +
                "weaker players in\n" +
                "unfair fights gives\n" +
                "you " + ChatColor.DARK_RED + "Bandit" + ChatColor.DARK_GRAY + " Points.\n\n" +
                ChatColor.DARK_GRAY + "At " + ChatColor.RED + "3 unfair kills\n" +
                ChatColor.DARK_GRAY + "you become a\n" +
                ChatColor.DARK_RED + "Bandit" + ChatColor.DARK_GRAY + ".\n\n" +
                ChatColor.DARK_GRAY + "Bandits are marked\n" +
                "with " + ChatColor.DARK_RED + "[B]" + ChatColor.DARK_GRAY + " and are\n" +
                "announced to all.";

        // Page 2: Redemption
        String page2 =
                ChatColor.GREEN + "" + ChatColor.BOLD + "REDEMPTION\n\n" +
                ChatColor.DARK_GRAY +
                "Bandits who kill\n" +
                "other bandits gain\n" +
                ChatColor.GREEN + "Redemption Points.\n\n" +
                ChatColor.DARK_GRAY +
                "At 3 bandits slain\n" +
                "while you are a\n" +
                "Bandit, you lose\n" +
                "your Bandit status\n" +
                "and return to\n" +
                "normal.\n\n";

        // Page 3: Heroes
        String page3 =
                ChatColor.GOLD + "" + ChatColor.BOLD + "HEROES\n\n" +
                ChatColor.DARK_GRAY +
                "Players who are\n" +
                "not Bandits and\n" +
                "kill Bandits gain\n" +
                ChatColor.GOLD + "Hero Points.\n\n" +
                ChatColor.DARK_GRAY +
                "At 3 bandits slain\n" +
                "while you are not\n" +
                "a Bandit, you\n" +
                "become a " +
                ChatColor.GOLD + "Hero" + ChatColor.DARK_GRAY + ".\n\n" +
                ChatColor.DARK_GRAY +
                "Heroes are marked\n" +
                "with " + ChatColor.GOLD + "[H]";

        // Page 4: Flow summary
        String page4 =
                ChatColor.GOLD + "" + ChatColor.BOLD + "STATUS FLOW\n\n" +
                ChatColor.DARK_GRAY +
                "Fair player:\n" +
                "  kill 3 bandits\n" +
                "  " + ChatColor.GREEN + "→ Hero\n\n" +
                ChatColor.DARK_GRAY +
                "Fair player:\n" +
                "  3 unfair kills\n" +
                "  " + ChatColor.DARK_RED + "→ Bandit\n\n" +
                ChatColor.DARK_GRAY +
                "Bandit:\n" +
                "  kill 3 bandits\n" +
                "  " + ChatColor.GREEN + "→ Redeemed\n\n" +
                ChatColor.DARK_GRAY +
                "Redeemed:\n" +
                "  kill 3 bandits\n" +
                "  as non-bandit\n" +
                "  " + ChatColor.GOLD + "→ Hero";

        meta.addPage(page1, page2, page3, page4);
        book.setItemMeta(meta);

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
            MessageStyler.sendPanel(player, "Guide Delivered",
                    ChatColor.YELLOW + "Your inventory was full.",
                    "The Bandits & Heroes guide was dropped at your feet.");
        } else {
            player.getInventory().addItem(book);
            MessageStyler.sendPanel(player, "Guide Delivered",
                    ChatColor.GOLD + "You received the " + ChatColor.DARK_RED + "Bandits & Heroes" + ChatColor.GOLD + " guide book.");
        }
    }
}
