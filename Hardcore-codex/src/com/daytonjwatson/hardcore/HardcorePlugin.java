package com.daytonjwatson.hardcore;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.daytonjwatson.hardcore.commands.GuideCommand;
import com.daytonjwatson.hardcore.commands.HelpCommand;
import com.daytonjwatson.hardcore.commands.RulesCommand;
import com.daytonjwatson.hardcore.commands.StatsCommand;
import com.daytonjwatson.hardcore.config.Config;
import com.daytonjwatson.hardcore.listeners.PlayerChatListener;
import com.daytonjwatson.hardcore.listeners.PlayerDeathListener;
import com.daytonjwatson.hardcore.listeners.PlayerJoinListener;
import com.daytonjwatson.hardcore.listeners.PlayerQuitListener;
import com.daytonjwatson.hardcore.managers.StatsManager;

public class HardcorePlugin extends JavaPlugin {

    public static HardcorePlugin instance;

    public static HardcorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize stats system (loads stats.yml, etc.)
        StatsManager.init(this);
        
        Config.setup();

        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        if (StatsManager.get() != null) {
            StatsManager.get().saveData(); // force save on shutdown
        }
    }

    private void registerCommands() {
    	getCommand("guide").setExecutor(new GuideCommand());
    	getCommand("help").setExecutor(new HelpCommand());
    	getCommand("rules").setExecutor(new RulesCommand());
    	
        if (getCommand("stats") != null) {
            StatsCommand statsCommand = new StatsCommand();
            getCommand("stats").setExecutor(statsCommand);
            getCommand("stats").setTabCompleter(statsCommand);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerChatListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
        pm.registerEvents(new PlayerDeathListener(), this);
        pm.registerEvents(new PlayerQuitListener(), this);
    }
}
