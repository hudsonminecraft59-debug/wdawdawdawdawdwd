package com.verdant.gui;

import java.util.ArrayList;
import java.util.List;

import com.verdant.config.CombatConfig;
import com.verdant.config.UtilityConfig;
import com.verdant.config.VerdantConfig;
import com.verdant.feature.UtilityModules;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** In-game overlay: watermark, coordinates, FPS, speed, and a list of enabled modules. */
public final class HudOverlay {
    private HudOverlay() {}

    private static final int ACCENT = 0xFF5CE0A8;
    private static final int TEXT = 0xFFE8F3EC;
    private static final int FAINT = 0xFF9DB5A8;

    public static void init() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("verdant", "overlay"), HudOverlay::render);
    }

    private static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!UtilityConfig.hud || mc.player == null || mc.options.hideGui) return;
        Font font = mc.font;

        int y = 4;
        g.drawString(font, "Verdant", 4, y, ACCENT, true);
        y += 11;
        if (UtilityConfig.hudCoords) {
            String coords = String.format("XYZ  %d  %d  %d", (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
            g.drawString(font, coords, 4, y, TEXT, true);
            y += 10;
        }
        if (UtilityConfig.hudFps) {
            g.drawString(font, mc.getFps() + " FPS", 4, y, FAINT, true);
            y += 10;
        }
        if (UtilityConfig.hudSpeed) {
            g.drawString(font, String.format("%.1f bps", UtilityModules.speed()), 4, y, FAINT, true);
        }

        if (UtilityConfig.hudModules) {
            List<String> names = new ArrayList<>();
            if (VerdantConfig.STORAGE.enabled) names.add("Storage ESP");
            if (VerdantConfig.SPAWNER.enabled) names.add("Spawn Finder");
            if (VerdantConfig.SUS.enabled) names.add("Sus Chunks");
            if (VerdantConfig.PLAYERS.enabled) names.add("Player ESP");
            if (CombatConfig.killAura) names.add("Kill Aura");
            if (CombatConfig.autoCrit) names.add("Auto Crit");
            if (UtilityConfig.autoTotem) names.add("Auto Totem");
            if (UtilityConfig.fullbright) names.add("Fullbright");
            if (UtilityConfig.autoSprint) names.add("Auto Sprint");
            names.sort((a, b) -> Integer.compare(font.width(b), font.width(a)));

            int screenW = mc.getWindow().getGuiScaledWidth();
            int ry = 4;
            for (String name : names) {
                int w = font.width(name);
                g.fill(screenW - w - 8, ry - 1, screenW - 5, ry + 9, 0x80081410);
                g.fill(screenW - 5, ry - 1, screenW - 3, ry + 9, ACCENT);
                g.drawString(font, name, screenW - w - 6, ry, TEXT, false);
                ry += 11;
            }
        }
    }
}
