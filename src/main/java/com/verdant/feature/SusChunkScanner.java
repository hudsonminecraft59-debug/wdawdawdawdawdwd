package com.verdant.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.verdant.config.VerdantConfig;
import com.verdant.config.VerdantConfig.Module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * "Sus chunks" are chunks whose player-made-looking blocks add up to a score
 * (a base or stash is likely there). It is a guess, not a guarantee:
 * villages and dungeons can also score points, so raise the threshold if you see false alarms.
 */
public final class SusChunkScanner {
    private SusChunkScanner() {}

    public record Sus(int chunkX, int chunkZ, double y, double score, int count, double distance) {}

    private static volatile List<Sus> found = List.of();
    private static final Set<Long> announced = new HashSet<>();
    private static ClientLevel lastLevel;
    private static int counter = 0;

    public static List<Sus> found() {
        return found;
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            found = List.of();
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            lastLevel = level;
            announced.clear();
        }

        Module module = VerdantConfig.SUS;
        if (!module.enabled || !VerdantConfig.SUS_CHUNK.enabled) {
            found = List.of();
            return;
        }

        counter++;
        if (counter % 20 != 0) return; // rescan once per second

        double px = player.getX();
        double pz = player.getZ();
        int playerChunkX = Mth.floor(px) >> 4;
        int playerChunkZ = Mth.floor(pz) >> 4;
        int radius = (int) Math.ceil(module.range / 16.0) + 1;

        List<Sus> result = new ArrayList<>();
        int newAlerts = 0;

        for (int cx = playerChunkX - radius; cx <= playerChunkX + radius; cx++) {
            for (int cz = playerChunkZ - radius; cz <= playerChunkZ + radius; cz++) {
                double dx = cx * 16 + 8 - px;
                double dz = cz * 16 + 8 - pz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > module.range) continue;

                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                double score = 0;
                int count = 0;
                double topY = Double.NEGATIVE_INFINITY;
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    double weight = weight(level.getBlockState(pos).getBlock());
                    if (weight <= 0) continue;
                    score += weight;
                    count++;
                    topY = Math.max(topY, pos.getY());
                }

                if (score < module.susThreshold) continue;

                result.add(new Sus(cx, cz, topY, score, count, dist));

                if (module.chatAlerts && newAlerts < 3) {
                    long key = (((long) cx) << 32) ^ (cz & 0xFFFFFFFFL);
                    if (announced.add(key)) {
                        newAlerts++;
                        player.displayClientMessage(Component.literal("[Verdant] Sus chunk at "
                                + (cx * 16 + 8) + ", " + (cz * 16 + 8)
                                + " (score " + Math.round(score) + ", " + count + " blocks)"), false);
                    }
                }
            }
        }
        found = result;
    }

    /** How much a block entity suggests that a player built something here. */
    private static double weight(Block block) {
        if (block instanceof ShulkerBoxBlock) return 5.0;   // never generates naturally
        if (block instanceof BeaconBlock) return 5.0;
        if (block instanceof HopperBlock) return 3.0;       // never generates naturally
        if (block instanceof EnderChestBlock) return 3.0;
        if (block instanceof DispenserBlock) return 1.0;    // also covers droppers
        if (block instanceof BrewingStandBlock) return 1.0;
        if (block instanceof ChestBlock) return 1.0;
        if (block instanceof BarrelBlock) return 1.0;
        if (block instanceof AbstractFurnaceBlock) return 0.5;
        if (block instanceof BedBlock) return 0.5;
        return 0.0;
    }
}
