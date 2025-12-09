package com.daytonjwatson.hardcore.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.BankGui;

public class BankCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can use bank commands."));
            return true;
        }

        BankManager bank = BankManager.get();

        if (args.length == 0) {
            BankGui.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "balance":
                double balance = bank.getBalance(player.getUniqueId());
                player.sendMessage(Util.color("&6Bank &8» &7Your balance: &a" + bank.formatCurrency(balance)));
                return true;
            case "transactions":
            case "history":
                BankGui.openTransactions(player);
                return true;
            case "send":
                if (args.length < 3) {
                    player.sendMessage(Util.color("&cUsage: /" + label + " send <player> <amount>"));
                    return true;
                }
                handleSend(player, args[1], args[2]);
                return true;
            default:
                player.sendMessage(Util.color("&cUnknown bank action. Try /bank, /bank balance, /bank transactions, or /bank send <player> <amount>."));
                return true;
        }
    }

    private void handleSend(Player sender, String targetName, String amountRaw) {
        BankManager bank = BankManager.get();
        OfflinePlayer target = findOfflinePlayerByName(targetName);

        if (target == null || target.getName() == null) {
            sender.sendMessage(Util.color("&cCould not find player '&f" + targetName + "&c'."));
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(Util.color("&cYou cannot send money to yourself."));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountRaw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Util.color("&c'" + amountRaw + "' is not a valid amount."));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(Util.color("&cAmount must be positive."));
            return;
        }

        UUID from = sender.getUniqueId();
        UUID to = target.getUniqueId();

        if (!bank.transfer(from, to, amount)) {
            sender.sendMessage(Util.color("&cYou don't have enough funds to send that amount."));
            return;
        }

        sender.sendMessage(Util.color("&aSent &f" + bank.formatCurrency(amount) + " &ato &e" + target.getName() + "&a."));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(Util.color("&aYou received &f" + bank.formatCurrency(amount) + " &afrom &e" + sender.getName() + "&a."));
        }
    }

    private OfflinePlayer findOfflinePlayerByName(String name) {
        OfflinePlayer onlineMatch = Bukkit.getPlayerExact(name);
        if (onlineMatch != null) {
            return onlineMatch;
        }

        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("balance");
            suggestions.add("transactions");
            suggestions.add("send");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
        }
        return suggestions;
    }
}
