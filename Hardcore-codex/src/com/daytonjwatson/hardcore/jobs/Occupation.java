package com.daytonjwatson.hardcore.jobs;

import java.util.List;

import org.bukkit.Material;

public enum Occupation {
    WARRIOR("Warrior", Material.IRON_SWORD, List.of(
            "Earn money for defeating hostile mobs.")),
    FARMER("Farmer", Material.WHEAT, List.of(
            "Harvest fully grown crops to get paid.")),
    FISHERMAN("Fisherman", Material.FISHING_ROD, List.of(
            "Reel in fish for steady income.")),
    LUMBERJACK("Lumberjack", Material.IRON_AXE, List.of(
            "Chop trees to collect wages.")),
    MINER("Miner", Material.DIAMOND_PICKAXE, List.of(
            "Break ore blocks for cash.")),
    EXPLORER("Explorer", Material.COMPASS, List.of(
            "Get rewarded for every block you travel.")),
    BUILDER("Builder", Material.BRICKS, List.of(
            "Place new block types to earn money."));

    private final String displayName;
    private final Material icon;
    private final List<String> defaultDescription;

    Occupation(String displayName, Material icon, List<String> defaultDescription) {
        this.displayName = displayName;
        this.icon = icon;
        this.defaultDescription = defaultDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDefaultDescription() {
        return defaultDescription;
    }

    public static Occupation fromString(String raw) {
        for (Occupation occupation : values()) {
            if (occupation.name().equalsIgnoreCase(raw) || occupation.displayName.equalsIgnoreCase(raw)) {
                return occupation;
            }
        }
        return null;
    }
}
