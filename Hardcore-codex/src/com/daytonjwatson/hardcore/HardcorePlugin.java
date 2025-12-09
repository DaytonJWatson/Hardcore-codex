package com.daytonjwatson.hardcore;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.daytonjwatson.hardcore.commands.BanditTrackerCommand;
import com.daytonjwatson.hardcore.commands.GuideCommand;
import com.daytonjwatson.hardcore.commands.HelpCommand;
import com.daytonjwatson.hardcore.commands.AdminCommand;
import com.daytonjwatson.hardcore.commands.RulesCommand;
import com.daytonjwatson.hardcore.commands.StatsCommand;
import com.daytonjwatson.hardcore.commands.BankCommand;
import com.daytonjwatson.hardcore.config.Config;
import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.listeners.PlayerChatListener;
import com.daytonjwatson.hardcore.listeners.PlayerDeathListener;
import com.daytonjwatson.hardcore.listeners.PlayerJoinListener;
import com.daytonjwatson.hardcore.listeners.PlayerQuitListener;
import com.daytonjwatson.hardcore.listeners.BanditTrackerListener;
import com.daytonjwatson.hardcore.listeners.FreezeListener;
import com.daytonjwatson.hardcore.listeners.BankGuiListener;
import com.daytonjwatson.hardcore.managers.BanditTrackerManager;
import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.AdminLogManager;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.DeathBanManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.listeners.PlayerLoginListener;

public class HardcorePlugin extends JavaPlugin {

    public static HardcorePlugin instance;

    public static HardcorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        Config.setup();

        AdminManager.init(this);
        AdminLogManager.init(this);
        BanManager.init(this);
        DeathBanManager.init(this);
        MuteManager.init(this);
        BankManager.init(this);

        // Initialize stats system (loads stats.yml, etc.)
        StatsManager.init(this);

        registerCommands();
        registerListeners();

        if (ConfigValues.trackerEnabled()) {
            BanditTrackerManager.registerRecipe(this);
        }
    }

    @Override
    public void onDisable() {
        if (StatsManager.get() != null) {
            StatsManager.get().saveData(); // force save on shutdown
        }

        AdminManager.save();
        AdminLogManager.save();
        BanManager.save();
        DeathBanManager.save();
        MuteManager.save();
        if (BankManager.get() != null) {
            BankManager.get().save();
        }

        instance = null;
    }

    private void registerCommands() {
        registerCommand("guide", new GuideCommand());
        registerCommand("help", new HelpCommand());
        registerCommand("rules", new RulesCommand());
        AdminCommand adminCommand = new AdminCommand();
        registerCommand("admin", adminCommand);
        registerTabCompleter("admin", adminCommand);
        // Bandit tracker is now crafted instead of granted via command; keep the
        // executor available in case legacy configs still register it.
        registerCommand("bandittracker", new BanditTrackerCommand());

        BankCommand bankCommand = new BankCommand();
        registerCommand("bank", bankCommand);
        registerTabCompleter("bank", bankCommand);
        registerCommand("pay", bankCommand);
        registerTabCompleter("pay", bankCommand);

        if (getCommand("stats") != null) {
            StatsCommand statsCommand = new StatsCommand();
            getCommand("stats").setExecutor(statsCommand);
            getCommand("stats").setTabCompleter(statsCommand);
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        if (getCommand(name) == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml");
            return;
        }

        getCommand(name).setExecutor(executor);
    }

    private void registerTabCompleter(String name, TabCompleter completer) {
        if (getCommand(name) == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml");
            return;
        }

        getCommand(name).setTabCompleter(completer);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerChatListener(), this);
        pm.registerEvents(new PlayerLoginListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
        pm.registerEvents(new PlayerDeathListener(), this);
        pm.registerEvents(new PlayerQuitListener(), this);
        pm.registerEvents(new BanditTrackerListener(), this);
        pm.registerEvents(new FreezeListener(), this);
        pm.registerEvents(new BankGuiListener(), this);
    }
}
