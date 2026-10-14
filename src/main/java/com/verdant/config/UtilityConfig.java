package com.verdant.config;

import java.util.Properties;

/** Settings for the Utility panel and the Auto Totem module. */
public final class UtilityConfig {
    private UtilityConfig() {}

    public static boolean fullbright = false;
    public static boolean autoSprint = false;
    public static boolean autoTotem = false;
    public static float totemHealth = 20f; // swap a totem into the off-hand when health <= this (20 = always)

    public static boolean hud = true;
    public static boolean hudModules = true;
    public static boolean hudCoords = true;
    public static boolean hudFps = true;
    public static boolean hudSpeed = true;

    static void reset() {
        fullbright = false;
        autoSprint = false;
        autoTotem = false;
        totemHealth = 20f;
        hud = true;
        hudModules = true;
        hudCoords = true;
        hudFps = true;
        hudSpeed = true;
    }

    static void load(Properties p) {
        fullbright = bool(p, "util.fullbright", fullbright);
        autoSprint = bool(p, "util.autoSprint", autoSprint);
        autoTotem = bool(p, "util.autoTotem", autoTotem);
        totemHealth = flt(p, "util.totemHealth", totemHealth, 2f, 20f);
        hud = bool(p, "util.hud", hud);
        hudModules = bool(p, "util.hudModules", hudModules);
        hudCoords = bool(p, "util.hudCoords", hudCoords);
        hudFps = bool(p, "util.hudFps", hudFps);
        hudSpeed = bool(p, "util.hudSpeed", hudSpeed);
    }

    static void save(Properties p) {
        p.setProperty("util.fullbright", String.valueOf(fullbright));
        p.setProperty("util.autoSprint", String.valueOf(autoSprint));
        p.setProperty("util.autoTotem", String.valueOf(autoTotem));
        p.setProperty("util.totemHealth", String.valueOf(totemHealth));
        p.setProperty("util.hud", String.valueOf(hud));
        p.setProperty("util.hudModules", String.valueOf(hudModules));
        p.setProperty("util.hudCoords", String.valueOf(hudCoords));
        p.setProperty("util.hudFps", String.valueOf(hudFps));
        p.setProperty("util.hudSpeed", String.valueOf(hudSpeed));
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
