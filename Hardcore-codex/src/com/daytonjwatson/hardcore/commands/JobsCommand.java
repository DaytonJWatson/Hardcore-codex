package com.daytonjwatson.hardcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.jobs.ActiveJob;
import com.daytonjwatson.hardcore.jobs.JobDefinition;
import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can use jobs."));
            return true;
        }

        JobsManager jobs = JobsManager.get();

        if (args.length == 0) {
            JobsGui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "accept":
                handleAccept(player, jobs, args, label);
                return true;
            case "abandon":
                jobs.abandonJob(player);
                return true;
            case "active":
            case "progress":
                showProgress(player, jobs);
                return true;
            case "offers":
                jobs.backupOffers(player);
                return true;
            case "reroll":
                jobs.rerollOffers(player.getUniqueId());
                player.sendMessage(Util.color("&aRefreshed your available contracts."));
                JobsGui.open(player);
                return true;
            default:
                player.sendMessage(Util.color("&cUnknown jobs action. Try /jobs, /jobs accept <1-3>, /jobs progress, /jobs reroll."));
                return true;
        }
    }

    private void handleAccept(Player player, JobsManager jobs, String[] args, String label) {
        if (jobs.getActiveJob(player.getUniqueId()) != null) {
            player.sendMessage(Util.color("&cYou already have an active job. Use /" + label + " abandon first."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Util.color("&cUsage: /" + label + " accept <1-3>"));
            return;
        }

        int choice;
        try {
            choice = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Util.color("&c'" + args[1] + "' is not a valid choice."));
            return;
        }

        if (choice < 1 || choice > 3) {
            player.sendMessage(Util.color("&cChoice must be between 1 and 3."));
            return;
        }

        List<JobDefinition> offers = jobs.getOfferedJobs(player.getUniqueId());
        if (offers.size() < choice) {
            player.sendMessage(Util.color("&cThat job offer is no longer available. Reroll and try again."));
            return;
        }

        jobs.assignJob(player, offers.get(choice - 1));
    }

    private void showProgress(Player player, JobsManager jobs) {
        ActiveJob active = jobs.getActiveJob(player.getUniqueId());
        if (active == null) {
            player.sendMessage(Util.color("&eYou don't have an active job. Use /jobs to pick one."));
            return;
        }

        JobDefinition def = active.getJob();
        MessageStyler.sendPanel(player, "Active Job",
                "&f" + def.getDisplayName(),
                "&7Progress: &f" + formatNumber(active.getProgress()) + "/" + formatNumber(def.getAmount()),
                "&7Target: &f" + def.getTarget(),
                "&7Reward: &a" + def.getReward());
    }

    private String formatNumber(double value) {
        if (value % 1 == 0) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("accept");
            options.add("abandon");
            options.add("offers");
            options.add("progress");
            options.add("reroll");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            options.add("1");
            options.add("2");
            options.add("3");
        }
        return options;
    }
}
