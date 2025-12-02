package com.daytonjwatson.hardcore.utils;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class TeleportUtil {
	private static final int RADIUS = 5000;
    private static final int MAX_ATTEMPTS = 40;

    public static void randomSafeTeleportNearSpawn(Player player) {
        World world = player.getWorld();
        Location spawn = world.getSpawnLocation();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

            int offsetX = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);
            int offsetZ = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);

            int x = spawn.getBlockX() + offsetX;
            int z = spawn.getBlockZ() + offsetZ;

            int groundY = world.getHighestBlockYAt(x, z); // surface

            if (groundY <= world.getMinHeight()) {
                continue;
            }

            Block ground = world.getBlockAt(x, groundY, z);
            Block feet = ground.getRelative(BlockFace.UP);
            Block head = feet.getRelative(BlockFace.UP);

            if (!isSurfaceSafe(ground, feet, head)) {
                continue;
            }

            // Feet one block above ground, centered on block
            Location tp = new Location(world, x + 0.5, groundY + 1.0, z + 0.5);
            player.teleport(tp);
            return;
        }

        MessageStyler.sendPanel(player, "Teleport Failed",
                ChatColor.RED + "Could not find a safe random location. Try again.");
    }

    private static boolean isSurfaceSafe(Block ground, Block feet, Block head) {
        Material g = ground.getType();
        Material f = feet.getType();
        Material h = head.getType();

        // Ground: solid and not tree/liquid/danger
        if (!g.isSolid()) return false;
        if (isTreeBlock(g)) return false;
        if (isDangerousBlock(g)) return false;

        // Feet / head: not solid and not obviously dangerous
        if (f.isSolid() || h.isSolid()) return false;
        if (isDangerousBlock(f) || isDangerousBlock(h)) return false;

        return true;
    }

    private static boolean isDangerousBlock(Material type) {
        switch (type) {
            case LAVA:
            case WATER:
            case FIRE:
            case SOUL_FIRE:
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
            case MAGMA_BLOCK:
            case CACTUS:
            case SWEET_BERRY_BUSH:
            case POWDER_SNOW:
                return true;
            default:
                return false;
        }
    }

    private static boolean isTreeBlock(Material type) {
        return Tag.LOGS.isTagged(type) || Tag.LEAVES.isTagged(type);
    }
}
