package com.daytonjwatson.hardcore.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AuctionHouseView;

public class AuctionHouseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can use the auction house."));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("add")) {
            handleAddListing(player, args);
            return true;
        }

        if (args.length >= 1) {
            try {
                int page = Integer.parseInt(args[0]);
                if (page <= 0) {
                    player.sendMessage(Util.color("&cPage numbers start at 1."));
                    return true;
                }
                AuctionHouseView.open(player, page - 1);
                return true;
            } catch (NumberFormatException ex) {
                player.sendMessage(Util.color("&cUnknown auction action. Try /auction, /auction <page>, or /auction add."));
                return true;
            }
        }

        AuctionHouseView.open(player);
        return true;
    }

    private void handleAddListing(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Util.color("&cUsage: /auction add <price-per-item> <quantity> (uses item in hand)"));
            return;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType() == Material.AIR) {
            player.sendMessage(Util.color("&cHold the item you want to list in your main hand."));
            return;
        }

        double price;
        int quantity;
        try {
            price = Double.parseDouble(args[1]);
            quantity = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Util.color("&cPrice and quantity must be numbers."));
            return;
        }

        if (price <= 0 || quantity <= 0) {
            player.sendMessage(Util.color("&cPrice and quantity must be greater than zero."));
            return;
        }

        if (inHand.getAmount() < quantity) {
            player.sendMessage(Util.color("&cYou don't have that many items in your hand."));
            return;
        }

        AuctionHouseManager manager = AuctionHouseManager.get();
        if (!AdminManager.isAdmin(player) && manager.getListingCount(player.getUniqueId()) >= 5) {
            player.sendMessage(Util.color("&cYou can only have 5 active auction listings at a time."));
            return;
        }

        ItemStack listingItem = inHand.clone();
        listingItem.setAmount(1);
        manager.addListing(player.getUniqueId(), listingItem, price, quantity);

        inHand.setAmount(inHand.getAmount() - quantity);
        player.getInventory().setItemInMainHand(inHand.getAmount() <= 0 ? new ItemStack(Material.AIR) : inHand);

        String priceDisplay = BankManager.get().formatCurrency(price);
        player.sendMessage(Util.color("&aListed &f" + quantity + "x &e" + Util.formatMaterialName(listingItem.getType())
                + " &afor &f" + priceDisplay + " &aeach."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            AuctionHouseManager manager = AuctionHouseManager.get();
            int totalPages = Math.max(1, (int) Math.ceil(manager.getListings().size() / (double) AuctionHouseView.pageSize()));
            for (int i = 1; i <= Math.min(totalPages, 5); i++) {
                suggestions.add(String.valueOf(i));
            }
            suggestions.add("add");
        }
        return suggestions;
    }
}
