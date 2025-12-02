package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.daytonjwatson.hardcore.managers.StatsManager;
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

		event.getPlayer().sendMessage(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "========== HARDCORE MODE ==========\n" +
                ChatColor.RED + "This server is one-life only.\n" +
                ChatColor.RED + "If you die, you are " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "PERMANENTLY BANNED.\n\n" +

                ChatColor.GRAY + "• PvP & Griefing is allowed but frowned upon.\n" +
                ChatColor.GRAY + "• Stealing is allowed.\n" +
                ChatColor.GRAY + "• There is no protection or land claiming.\n" +
                ChatColor.GRAY + "• Trust nobody — alliances are not enforced.\n\n" +

                ChatColor.DARK_GRAY + "Play smart. Play cautiously.\n" +
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "You only get one life."
        );
		
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
