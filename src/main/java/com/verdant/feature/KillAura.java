package com.verdant.feature;

import com.verdant.config.CombatConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Attacks the closest valid target in reach. Auto Crit makes it wait until the
 * player is falling (that is when a jump-attack becomes a critical hit) and can
 * jump by itself to set that up.
 */
public final class KillAura {
    private KillAura() {}

    private static boolean jumpHeldByMod = false;

    public static void tick(Minecraft mc) {
        // release the jump key we pressed on the previous tick
        if (jumpHeldByMod) {
            mc.options.keyJump.setDown(false);
            jumpHeldByMod = false;
        }

        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (!CombatConfig.killAura || player == null || level == null || mc.gameMode == null) return;
        if (mc.screen != null || !player.isAlive() || player.isSpectator()) return;

        Entity target = findTarget(player, level);
        if (target == null) return;

        boolean ready = !CombatConfig.fullCooldown || player.getAttackStrengthScale(0.5f) >= 0.95f;
        if (!ready) return;

        if (CombatConfig.autoCrit) {
            if (CombatConfig.autoJump && player.onGround() && !player.isInWater() && !player.onClimbable()) {
                mc.options.keyJump.setDown(true);
                jumpHeldByMod = true;
                return;
            }
            if (!canCrit(player)) return;
        }

        if (CombatConfig.rotate) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        }
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean canCrit(LocalPlayer player) {
        return player.fallDistance > 0.0
                && !player.onGround()
                && !player.isInWater()
                && !player.onClimbable()
                && !player.isPassenger();
    }

    private static Entity findTarget(LocalPlayer player, ClientLevel level) {
        double bestScore = Double.MAX_VALUE;
        Entity bestEntity = null;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            boolean wanted;
            if (entity instanceof Player other) {
                wanted = CombatConfig.hitPlayers && !other.isSpectator();
            } else if (entity instanceof Enemy) {
                wanted = CombatConfig.hitHostiles;
            } else if (entity instanceof Animal) {
                wanted = CombatConfig.hitPassives;
            } else {
                wanted = false;
            }
            if (!wanted) continue;

            double distance = player.distanceTo(entity);
            if (distance > CombatConfig.reach) continue;
            double score = CombatConfig.lowestHealthFirst ? living.getHealth() : distance;
            if (score < bestScore) {
                bestScore = score;
                bestEntity = entity;
            }
        }
        return bestEntity;
    }
}
