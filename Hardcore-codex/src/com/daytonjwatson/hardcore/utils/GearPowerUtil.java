package com.daytonjwatson.hardcore.utils;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GearPowerUtil {

    public static final class CombatSnapshot {
        public final int gearPower;
        public final double attributePower;
        public final double potionPower;
        public final double totalPower;

        public CombatSnapshot(int gearPower, double attributePower, double potionPower, double totalPower) {
            this.gearPower = gearPower;
            this.attributePower = attributePower;
            this.potionPower = potionPower;
            this.totalPower = totalPower;
        }
    }

    // ====================== CONFIG / WEIGHTS ======================

    // These you already had (or similar):
    private static final boolean COUNT_ENCHANTS = true;

    private static final double ARMOR_BASE_WEIGHT   = 1.0;
    private static final double WEAPON_BASE_WEIGHT  = 1.2;
    private static final double SHIELD_BASE_WEIGHT  = 0.9;

    private static final int HELMET_MULTIPLIER     = 2;
    private static final int CHESTPLATE_MULTIPLIER = 4;
    private static final int LEGGINGS_MULTIPLIER   = 3;
    private static final int BOOTS_MULTIPLIER      = 1;

    private static final double ENCHANT_OFFENSE_WEIGHT = 0.45;
    private static final double ENCHANT_DEFENSE_WEIGHT = 0.35;
    private static final double ENCHANT_UTILITY_WEIGHT = 0.20;

    // NEW: attribute weights
    private static final double ATTR_ARMOR_WEIGHT          = 1.0;
    private static final double ATTR_ARMOR_TOUGHNESS_WEIGHT= 1.5;
    private static final double ATTR_MAX_HEALTH_WEIGHT     = 0.75; // per HP above 20
    private static final double ATTR_ATTACK_DAMAGE_WEIGHT  = 1.0;  // per damage above baseline
    private static final double ATTR_MOVE_SPEED_WEIGHT     = 15.0; // movement speed is small (~0.1), so big coeff

    // NEW: potion weights
    private static final double POTION_STRENGTH_WEIGHT     = 2.5;  // per amplifier
    private static final double POTION_RESISTANCE_WEIGHT   = 2.0;  // per amplifier
    private static final double POTION_SPEED_WEIGHT        = 1.5;
    private static final double POTION_REGEN_WEIGHT        = 2.0;
    private static final double POTION_ABSORPTION_WEIGHT   = 0.6;  // per HP of absorption

    // How much we let attributes + potions matter compared to gear
    private static final double ATTRIBUTES_GLOBAL_WEIGHT   = 1.0;
    private static final double POTIONS_GLOBAL_WEIGHT      = 0.6;  // buffs count, but less than permanent stuff

    // ==============================================================
    // PUBLIC API
    // ==============================================================

    /**
     * Legacy/simple API: still exposed so existing code doesn’t break.
     * This is just the gear portion (items + enchants).
     */
    public static int getPlayerPower(Player player) {
        CombatSnapshot snap = getCombatSnapshot(player);
        return snap.gearPower;
    }

    /**
     * Full combat snapshot including gear, attributes, and potion effects.
     */
    public static CombatSnapshot getCombatSnapshot(Player player) {
        PlayerInventory inv = player.getInventory();

        // ---------------- Gear block (same as your advanced system) ----------------
        int armorScore = 0;
        armorScore += getArmorTier(inv.getHelmet())     * HELMET_MULTIPLIER;
        armorScore += getArmorTier(inv.getChestplate()) * CHESTPLATE_MULTIPLIER;
        armorScore += getArmorTier(inv.getLeggings())   * LEGGINGS_MULTIPLIER;
        armorScore += getArmorTier(inv.getBoots())      * BOOTS_MULTIPLIER;

        int weaponScore = getWeaponScore(inv.getItemInMainHand());
        int shieldScore = getShieldScore(inv);

        double gearTotal = 0.0;
        gearTotal += armorScore * ARMOR_BASE_WEIGHT;
        gearTotal += weaponScore * WEAPON_BASE_WEIGHT;
        gearTotal += shieldScore * SHIELD_BASE_WEIGHT;

        if (COUNT_ENCHANTS) {
            double enchantScore = getEnchantScore(inv);
            gearTotal += enchantScore;
        }

        int gearPower = (int) Math.round(gearTotal);

        // ---------------- Attributes block ----------------
        double attrPower = getAttributePower(player) * ATTRIBUTES_GLOBAL_WEIGHT;

        // ---------------- Potions block ----------------
        double potionPower = getPotionPower(player) * POTIONS_GLOBAL_WEIGHT;

        double total = gearPower + attrPower + potionPower;

        return new CombatSnapshot(gearPower, attrPower, potionPower, total);
    }

    // ==============================================================
    // ATTRIBUTES
    // ==============================================================

    private static double getAttributePower(Player player) {
        double score = 0.0;

        // Armor
        double armor = getAttr(player, Attribute.ARMOR);
        double toughness = getAttr(player, Attribute.ARMOR_TOUGHNESS);
        score += armor * ATTR_ARMOR_WEIGHT;
        score += toughness * ATTR_ARMOR_TOUGHNESS_WEIGHT;

        // Max health – only count above vanilla 20.0
        double maxHealth = getAttr(player, Attribute.MAX_HEALTH);
        if (maxHealth > 20.0) {
            score += (maxHealth - 20.0) * ATTR_MAX_HEALTH_WEIGHT;
        }

        // Attack damage – only count above baseline (1.0)
        double attackDamage = getAttr(player, Attribute.ATTACK_DAMAGE);
        if (attackDamage > 1.0) {
            score += (attackDamage - 1.0) * ATTR_ATTACK_DAMAGE_WEIGHT;
        }

        // Movement speed – relative to ~0.1 (typical player)
        double moveSpeed = getAttr(player, Attribute.MOVEMENT_SPEED);
        double baselineSpeed = 0.1;
        if (moveSpeed > baselineSpeed) {
            score += (moveSpeed - baselineSpeed) * ATTR_MOVE_SPEED_WEIGHT;
        }

        return score;
    }

    private static double getAttr(Player player, Attribute attr) {
        if (player.getAttribute(attr) == null) return 0.0;
        return player.getAttribute(attr).getValue();
    }

    // ==============================================================
    // POTIONS
    // ==============================================================

    private static double getPotionPower(Player player) {
        double score = 0.0;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            int amp = effect.getAmplifier() + 1; // amplifiers are 0-based

            if (type.equals(PotionEffectType.STRENGTH)) { // Strength
                score += amp * POTION_STRENGTH_WEIGHT;
            } else if (type.equals(PotionEffectType.RESISTANCE)) {
                score += amp * POTION_RESISTANCE_WEIGHT;
            } else if (type.equals(PotionEffectType.SPEED)) {
                score += amp * POTION_SPEED_WEIGHT;
            } else if (type.equals(PotionEffectType.REGENERATION)) {
                score += amp * POTION_REGEN_WEIGHT;
            }
        }

        // Absorption is not a PotionEffectType in all APIs, so read directly:
        double absorptionHearts = player.getAbsorptionAmount(); // in HP (2.0 = 1 heart)
        if (absorptionHearts > 0) {
            score += absorptionHearts * POTION_ABSORPTION_WEIGHT;
        }

        return score;
    }

    // ==============================================================
    // GEAR / ENCHANT CODE (same as previous advanced version, trimmed)
    // ==============================================================

    private static int getArmorTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        Material type = item.getType();

        switch (type) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return 1;

            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case COPPER_HELMET:
            case COPPER_CHESTPLATE:
            case COPPER_LEGGINGS:
            case COPPER_BOOTS:
                return 2;

            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                return 3;

            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                return 4;

            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                return 5;

            default:
                return 0;
        }
    }

    private static int getWeaponScore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        Material type = item.getType();

        switch (type) {
            case WOODEN_SWORD:    return 1;
            case STONE_SWORD:     return 2;
            case GOLDEN_SWORD:    return 2;
            case COPPER_SWORD:    return 2;
            case IRON_SWORD:      return 3;
            case DIAMOND_SWORD:   return 4;
            case NETHERITE_SWORD: return 5;

            case WOODEN_AXE:      return 1;
            case STONE_AXE:       return 2;
            case GOLDEN_AXE:      return 2;
            case COPPER_AXE:      return 2;
            case IRON_AXE:        return 3;
            case DIAMOND_AXE:     return 4;
            case NETHERITE_AXE:   return 5;

            case BOW:             return 3;
            case CROSSBOW:        return 3;
            case TRIDENT:         return 4;

            default:
                return 0;
        }
    }

    private static int getShieldScore(PlayerInventory inv) {
        ItemStack main = inv.getItemInMainHand();
        ItemStack off  = inv.getItemInOffHand();

        boolean hasShield =
                (main != null && main.getType() == Material.SHIELD) ||
                (off != null && off.getType() == Material.SHIELD);

        return hasShield ? 3 : 0;
    }

    private static double getEnchantScore(PlayerInventory inv) {
        double offense = 0.0;
        double defense = 0.0;
        double utility = 0.0;

        ItemStack[] armor = new ItemStack[] {
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        };

        for (ItemStack piece : armor) {
            offense += getOffensiveEnchantScore(piece);
            defense += getDefensiveEnchantScore(piece);
            utility += getUtilityEnchantScore(piece);
        }

        ItemStack main = inv.getItemInMainHand();
        offense += getOffensiveEnchantScore(main);
        defense += getDefensiveEnchantScore(main);
        utility += getUtilityEnchantScore(main);

        ItemStack off = inv.getItemInOffHand();
        offense += getOffensiveEnchantScore(off);
        defense += getDefensiveEnchantScore(off);
        utility += getUtilityEnchantScore(off);

        return offense * ENCHANT_OFFENSE_WEIGHT
             + defense * ENCHANT_DEFENSE_WEIGHT
             + utility * ENCHANT_UTILITY_WEIGHT;
    }

    private static int getOffensiveEnchantScore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        int score = 0;

        score += getLevel(item, Enchantment.SHARPNESS);
        score += getLevel(item, Enchantment.SMITE);
        score += getLevel(item, Enchantment.BANE_OF_ARTHROPODS);
        score += getLevel(item, Enchantment.POWER);
        score += getLevel(item, Enchantment.PUNCH);
        score += getLevel(item, Enchantment.IMPALING);

        return score;
    }

    private static int getDefensiveEnchantScore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        int score = 0;

        score += getLevel(item, Enchantment.PROTECTION);
        score += getLevel(item, Enchantment.BLAST_PROTECTION);
        score += getLevel(item, Enchantment.PROJECTILE_PROTECTION);
        score += getLevel(item, Enchantment.FIRE_PROTECTION);
        score += getLevel(item, Enchantment.THORNS);
        score += getLevel(item, Enchantment.FEATHER_FALLING);

        return score;
    }

    private static int getUtilityEnchantScore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        int score = 0;

        score += getLevel(item, Enchantment.UNBREAKING);
        score += getLevel(item, Enchantment.MENDING);
        return score;
    }

    private static int getLevel(ItemStack item, Enchantment ench) {
        if (item == null) return 0;
        return item.getEnchantmentLevel(ench);
    }
}
