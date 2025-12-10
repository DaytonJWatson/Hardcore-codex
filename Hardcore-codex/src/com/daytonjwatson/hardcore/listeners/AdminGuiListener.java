package com.daytonjwatson.hardcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AdminGui;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;

public class AdminGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(AdminGui.TITLE)) {
            return;
        }

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String command = AdminGui.getCommand(current);
        if (command == null || command.isEmpty()) {
            return;
        }

        player.closeInventory();

        TextComponent message = new TextComponent(Util.color("&6Admin GUI &8» &7Click to run: &e" + command));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Util.color("&7Click to paste this command"))
                        .create()));
        player.spigot().sendMessage(message);

        if (AdminGui.shouldExecuteDirect(current)) {
            String raw = command.startsWith("/") ? command.substring(1) : command;
            if (!raw.isBlank()) {
                player.performCommand(raw);
                player.sendMessage(Util.color("&7(Executed directly; edit chat suggestion to change)"));
            }
        }
    }
}
