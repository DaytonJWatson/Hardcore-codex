package com.daytonjwatson.hardcore.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FreezeManager {

    private static final Map<UUID, String> frozenReasons = new HashMap<>();

    private FreezeManager() {
    }

    public static void freeze(Player player, String reason) {
        frozenReasons.put(player.getUniqueId(), reason);
    }

    public static boolean unfreeze(UUID uuid) {
        return frozenReasons.remove(uuid) != null;
    }

    public static boolean isFrozen(UUID uuid) {
        return frozenReasons.containsKey(uuid);
    }

    public static String getReason(UUID uuid) {
        return frozenReasons.getOrDefault(uuid, "Frozen by an admin.");
    }

    public static List<String> getFrozenNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isFrozen(player.getUniqueId())) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
