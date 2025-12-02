package com.daytonjwatson.hardcore.config;

import com.daytonjwatson.hardcore.HardcorePlugin;

public class Config {
	
	public static void setup() {
		create();
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
	}
	
}
