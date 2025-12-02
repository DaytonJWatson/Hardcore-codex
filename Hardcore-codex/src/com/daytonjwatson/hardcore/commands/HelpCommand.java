package com.daytonjwatson.hardcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.utils.MessageStyler;

public class HelpCommand implements CommandExecutor {
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        var lines = ConfigValues.helpLines();
        MessageStyler.sendPanel(sender, ConfigValues.translateColor(ConfigValues.helpTitle()),
                lines.toArray(new String[0]));

        return true;
    }
}
