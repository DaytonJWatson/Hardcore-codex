package com.daytonjwatson.hardcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.jobs.Occupation;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationSettings;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can manage occupations."));
            return true;
        }

        JobsManager jobs = JobsManager.get();

        if (args.length == 0) {
            JobsGui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set":
                setOccupation(player, jobs, args, label);
                return true;
            case "clear":
                jobs.clearOccupation(player);
                return true;
            case "info":
            case "current":
                showInfo(player, jobs);
                return true;
            default:
                player.sendMessage(Util.color("&cUnknown action. Try /" + label + " set <occupation> or /" + label
                        + " info."));
                return true;
        }
    }

    private void setOccupation(Player player, JobsManager jobs, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(Util.color("&cUsage: /" + label + " set <occupation>"));
            return;
        }
        Occupation occupation = Occupation.fromString(args[1]);
        if (occupation == null) {
            player.sendMessage(Util.color("&cUnknown occupation. Valid options: warrior, farmer, fisherman, lumberjack, miner, explorer, builder."));
            return;
        }
        jobs.setOccupation(player, occupation);
    }

    private void showInfo(Player player, JobsManager jobs) {
        Occupation occupation = jobs.getOccupation(player.getUniqueId());
        if (occupation == null) {
            player.sendMessage(Util.color("&eYou do not have an occupation yet. Use /jobs to pick one."));
            return;
        }

        OccupationSettings settings = jobs.getOccupationSettings().get(occupation);
        List<String> lines = new ArrayList<>();
        lines.add("&f" + settings.displayName());
        for (String line : settings.description()) {
            lines.add("&7" + line);
        }
        switch (occupation) {
            case WARRIOR -> lines.add("&fHostile mobs: &a+" + settings.killHostileReward());
            case FARMER -> lines.add("&fHarvested crop: &a+" + settings.harvestReward());
            case FISHERMAN -> lines.add("&fCaught fish: &a+" + settings.catchReward());
            case LUMBERJACK -> lines.add("&fLog broken: &a+" + settings.logReward());
            case MINER -> lines.add("&fOre mined: &a+" + settings.oreReward());
            case EXPLORER -> lines.add("&fTravel: &a+" + settings.travelRewardPerBlock() + " per block");
            case BUILDER -> lines.add("&fNew block type: &a+" + settings.uniqueBlockReward());
        }
        MessageStyler.sendPanel(player, "Occupation", lines.toArray(new String[0]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("set");
            options.add("clear");
            options.add("info");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (Occupation occupation : Occupation.values()) {
                options.add(occupation.name().toLowerCase(Locale.ROOT));
            }
        }
        return options;
    }
}
