package com.verdant.feature;

import com.verdant.config.UtilityConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Fullbright, Auto Sprint, Auto Totem, and the speed readout for the HUD. */
public final class UtilityModules {
    private UtilityModules() {}

    private static boolean nightVisionApplied = false;
    private static boolean sprintHeldByMod = false;
    private static int totemCooldown = 0;

    private static double lastX;
    private static double lastZ;
    private static boolean hasLast = false;
    private static double speed = 0.0; // blocks per second

    public static double speed() {
        return speed;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            hasLast = false;
            nightVisionApplied = false;
            sprintHeldByMod = false;
            return;
        }

        trackSpeed(player);
        fullbright(player);
        autoSprint(mc);
        autoTotem(mc, player);
    }

    private static void trackSpeed(LocalPlayer player) {
        double x = player.getX();
        double z = player.getZ();
        if (hasLast) {
            double now = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ)) * 20.0;
            speed = speed * 0.6 + now * 0.4; // smooth it a little
        }
        lastX = x;
        lastZ = z;
        hasLast = true;
    }

    /** Night vision effect that only exists on your own client. */
    private static void fullbright(LocalPlayer player) {
        if (UtilityConfig.fullbright) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 400) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4000, 0, false, false));
            }
            nightVisionApplied = true;
        } else if (nightVisionApplied) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            nightVisionApplied = false;
        }
    }

    private static void autoSprint(Minecraft mc) {
        boolean wantSprint = UtilityConfig.autoSprint && mc.screen == null && mc.options.keyUp.isDown();
        if (wantSprint) {
            mc.options.keySprint.setDown(true);
            sprintHeldByMod = true;
        } else if (sprintHeldByMod) {
            mc.options.keySprint.setDown(false);
            sprintHeldByMod = false;
        }
    }

    private static void autoTotem(Minecraft mc, LocalPlayer player) {
        if (totemCooldown > 0) totemCooldown--;
        if (!UtilityConfig.autoTotem || mc.gameMode == null || mc.screen != null) return;
        if (totemCooldown > 0) return;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (player.getHealth() > UtilityConfig.totemHealth) return;

        AbstractContainerMenu menu = player.inventoryMenu;
        for (int slot = 9; slot <= 44; slot++) { // main inventory + hotbar
            if (menu.getSlot(slot).getItem().is(Items.TOTEM_OF_UNDYING)) {
                // button 40 = swap with the off-hand, the same as pressing F over the item
                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 40, ClickType.SWAP, player);
                totemCooldown = 5;
                return;
            }
        }
    }
}
