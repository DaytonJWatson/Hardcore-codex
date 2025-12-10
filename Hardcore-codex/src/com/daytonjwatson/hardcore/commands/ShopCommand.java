package com.daytonjwatson.hardcore.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.ShopManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.ShopBrowserView;
import com.daytonjwatson.hardcore.views.ShopManagerView;

public class ShopCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        ShopManager.init(com.daytonjwatson.hardcore.HardcorePlugin.getInstance());

        if (args.length > 0 && args[0].equalsIgnoreCase("manage")) {
            ShopManagerView.open(player);
            return true;
        }

        ShopBrowserView.open(player, 0);
        player.sendMessage(Util.color("&7Browse shops or use &f/shops manage &7to manage yours."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "manage".startsWith(args[0].toLowerCase())) {
            return List.of("manage");
        }
        return List.of();
    }
}
