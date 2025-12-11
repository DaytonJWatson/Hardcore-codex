package com.daytonjwatson.hardcore.config;

import com.daytonjwatson.hardcore.HardcorePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import com.daytonjwatson.hardcore.config.ConfigValues;

public class Config {

        public static void setup() {
                create();
                FileConfiguration configuration = HardcorePlugin.instance.getConfig();
                ConfigValues.load(configuration);
        }

        private static void create() {
                HardcorePlugin.instance.getConfig().options().copyDefaults(true);
                HardcorePlugin.instance.saveDefaultConfig();
	}
	
	public static void save() {
		HardcorePlugin.instance.saveConfig();
	}
	
        public static void reload() {
                HardcorePlugin.instance.reloadConfig();
                ConfigValues.load(HardcorePlugin.instance.getConfig());
        }

}
