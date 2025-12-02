package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.TabUtil;
import com.daytonjwatson.hardcore.utils.TeleportUtil;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerJoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		StatsManager stats = StatsManager.get();
		
		if(player.hasPlayedBefore())
			existingPlayer(player, event);
		else
			newPlayer(player, event);

                MessageStyler.sendPanel(event.getPlayer(), "Hardcore Mode",
                        ChatColor.RED + "One-life only. Death = Permanent Ban.",
                        "PvP & Griefing allowed but frowned upon.",
                        "Stealing is allowed.",
                        "No protection or land claiming.",
                        "Trust nobody — alliances are not enforced.",
                        ChatColor.DARK_RED + "Play smart. You only get one life.");
		
		stats.handleJoin(player);
        
		TabUtil.updateTabForAll();
	}
	
	private void existingPlayer(Player player, PlayerJoinEvent event) {
		event.setJoinMessage(Util.color("&7" + player.getName() + " has &ajoined"));
	}
	
	private void newPlayer(Player player, PlayerJoinEvent event) {
		event.setJoinMessage(Util.color("&7" + player.getName() + " has &ajoined &7for the first time!"));
		TeleportUtil.randomSafeTeleportNearSpawn(player);
		Util.giveHardcoreGuideBook(player);
	}
}
