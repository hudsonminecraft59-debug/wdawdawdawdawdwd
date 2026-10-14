package com.verdant.config;

import java.util.Properties;

/** Settings for the Combat panel (Kill Aura and Auto Crit). Stored in the same file as VerdantConfig. */
public final class CombatConfig {
    private CombatConfig() {}

    // Kill Aura
    public static boolean killAura = false;
    public static float reach = 3.0f;
    public static boolean hitPlayers = true;
    public static boolean hitHostiles = true;
    public static boolean hitPassives = false;
    public static boolean fullCooldown = true;
    public static boolean rotate = false;
    public static boolean lowestHealthFirst = false;

    // Auto Crit (works together with Kill Aura)
    public static boolean autoCrit = false;
    public static boolean autoJump = false;

    static void reset() {
        killAura = false;
        reach = 3.0f;
        hitPlayers = true;
        hitHostiles = true;
        hitPassives = false;
        fullCooldown = true;
        rotate = false;
        lowestHealthFirst = false;
        autoCrit = false;
        autoJump = false;
    }

    static void load(Properties p) {
        killAura = bool(p, "combat.killAura", killAura);
        reach = flt(p, "combat.reach", reach, 2f, 6f);
        hitPlayers = bool(p, "combat.hitPlayers", hitPlayers);
        hitHostiles = bool(p, "combat.hitHostiles", hitHostiles);
        hitPassives = bool(p, "combat.hitPassives", hitPassives);
        fullCooldown = bool(p, "combat.fullCooldown", fullCooldown);
        rotate = bool(p, "combat.rotate", rotate);
        lowestHealthFirst = bool(p, "combat.lowestHealthFirst", lowestHealthFirst);
        autoCrit = bool(p, "combat.autoCrit", autoCrit);
        autoJump = bool(p, "combat.autoJump", autoJump);
    }

    static void save(Properties p) {
        p.setProperty("combat.killAura", String.valueOf(killAura));
        p.setProperty("combat.reach", String.valueOf(reach));
        p.setProperty("combat.hitPlayers", String.valueOf(hitPlayers));
        p.setProperty("combat.hitHostiles", String.valueOf(hitHostiles));
        p.setProperty("combat.hitPassives", String.valueOf(hitPassives));
        p.setProperty("combat.fullCooldown", String.valueOf(fullCooldown));
        p.setProperty("combat.rotate", String.valueOf(rotate));
        p.setProperty("combat.lowestHealthFirst", String.valueOf(lowestHealthFirst));
        p.setProperty("combat.autoCrit", String.valueOf(autoCrit));
        p.setProperty("combat.autoJump", String.valueOf(autoJump));
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static float flt(Properties p, String key, float def, float min, float max) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            return Math.max(min, Math.min(max, Float.parseFloat(v.trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
