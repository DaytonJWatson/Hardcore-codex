package com.daytonjwatson.hardcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.jobs.Occupation;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationProfile;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationSettings;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, label, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can manage occupations."));
            return true;
        }

        if (args.length == 0) {
            JobsGui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set":
                selectOccupation(player, args, label);
                return true;
            case "info":
            case "current":
                showInfo(player);
                return true;
            default:
                player.sendMessage(Util.color("&cUnknown action. Try /" + label
                        + " set <occupation> or open the GUI with /" + label + "."));
                return true;
        }
    }

    private void selectOccupation(Player player, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(Util.color("&cUsage: /" + label + " set <occupation>"));
            return;
        }
        if (JobsManager.get().getOccupation(player.getUniqueId()) != null) {
            player.sendMessage(Util.color("&cYour occupation is permanent and already chosen."));
            return;
        }
        Occupation occupation = Occupation.fromString(args[1]);
        if (occupation == null) {
            player.sendMessage(Util.color(
                    "&cUnknown occupation. Valid options: warrior, farmer, fisherman, lumberjack, miner, explorer, builder."));
            return;
        }
        JobsManager.get().queueSelection(player, occupation);
        JobsGui.openConfirm(player, occupation);
    }

    private boolean handleAdmin(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(JobsManager.ADMIN_PERMISSION)) {
            sender.sendMessage(Util.color("&cYou do not have permission to manage occupations."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Util.color("&cUsage: /" + label + " admin <view|set|reset> <player> [occupation]"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer must be online for admin actions."));
            return true;
        }
        JobsManager jobs = JobsManager.get();
        switch (action) {
            case "view" -> {
                OccupationProfile profile = jobs.getProfile(target.getUniqueId());
                if (profile == null) {
                    sender.sendMessage(Util.color("&e" + target.getName() + " has no occupation yet."));
                    return true;
                }
                OccupationSettings settings = jobs.getOccupationSettings().get(profile.getOccupation());
                sender.sendMessage(Util.color("&f" + target.getName() + " &7- &f" + settings.displayName()
                        + " &7| Lifetime: &a$" + profile.getLifetimeEarnings() + " &7Session: &a$"
                        + profile.getSessionEarnings()));
                return true;
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(Util.color("&cUsage: /" + label + " admin set <player> <occupation>"));
                    return true;
                }
                Occupation occupation = Occupation.fromString(args[3]);
                if (occupation == null) {
                    sender.sendMessage(Util.color("&cUnknown occupation."));
                    return true;
                }
                jobs.setOccupation(target, occupation, true);
                sender.sendMessage(Util.color("&aSet " + target.getName() + " to " + occupation.getDisplayName()));
                return true;
            }
            case "reset" -> {
                jobs.resetOccupation(target.getUniqueId());
                sender.sendMessage(Util.color("&eCleared occupation for " + target.getName()));
                target.sendMessage(Util.color("&cAn administrator cleared your occupation. Please choose again."));
                JobsGui.open(target);
                return true;
            }
            default -> {
                sender.sendMessage(Util.color("&cUnknown admin action."));
                return true;
            }
        }
    }

    private void showInfo(Player player) {
        Occupation occupation = JobsManager.get().getOccupation(player.getUniqueId());
        if (occupation == null) {
            player.sendMessage(Util.color("&eYou do not have an occupation yet. Use /jobs to pick one."));
            return;
        }

        OccupationSettings settings = JobsManager.get().getOccupationSettings().get(occupation);
        List<String> lines = new ArrayList<>();
        lines.add("&f" + settings.displayName());
        for (String line : settings.description()) {
            lines.add("&7" + line);
        }
        switch (occupation) {
            case WARRIOR -> {
                lines.add("&fPvP kill: &a+" + settings.getReward("pvp-kill"));
                lines.add("&fHostile kill: &a+" + settings.getReward("hostile-kill"));
            }
            case FARMER -> lines.add("&fHarvest: &a+" + settings.getReward("harvest"));
            case FISHERMAN -> lines.add("&fCatch: &a+" + settings.getReward("catch"));
            case LUMBERJACK -> lines.add("&fTree: &a+" + settings.getReward("tree"));
            case MINER -> {
                lines.add("&fOre: &a+" + settings.getReward("ore"));
                lines.add("&fDepth bonus: &a+" + settings.getReward("depth-bonus"));
            }
            case EXPLORER -> lines.add("&fNew chunk: &a+" + settings.getReward("chunk"));
            case BUILDER -> lines.add("&fBuild session: &a+" + settings.getReward("build-session"));
        }
        MessageStyler.sendPanel(player, "Occupation", lines.toArray(new String[0]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("set");
            options.add("info");
            options.add("admin");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (Occupation occupation : Occupation.values()) {
                options.add(occupation.name().toLowerCase(Locale.ROOT));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            options.add("view");
            options.add("set");
            options.add("reset");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("set")) {
            for (Occupation occupation : Occupation.values()) {
                options.add(occupation.name().toLowerCase(Locale.ROOT));
            }
        }
        return options;
    }
}
