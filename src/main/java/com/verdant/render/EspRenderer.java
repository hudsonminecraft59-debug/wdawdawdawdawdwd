package com.verdant.render;

import java.util.List;

import com.verdant.config.VerdantConfig;
import com.verdant.config.VerdantConfig.Module;
import com.verdant.feature.SusChunkScanner;
import com.verdant.feature.SusChunkScanner.Sus;
import com.verdant.feature.TargetScanner;
import com.verdant.feature.TargetScanner.Hit;

import org.joml.Matrix4fc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws outlines (thin boxes along each edge), fills and tracer lines for every found block / chunk. */
public final class EspRenderer {
    private EspRenderer() {}

    private static final BoxBuffer THROUGH_WALLS = new BoxBuffer("through_walls", true);
    private static final BoxBuffer DEPTH_TESTED = new BoxBuffer("depth_tested", false);

    /** How far in front of the camera tracers start, so they seem to come from the crosshair. */
    private static final double TRACER_START = 0.8;

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            THROUGH_WALLS.close();
            DEPTH_TESTED.close();
        });
    }

    private static void render(WorldRenderContext context) {
        List<Hit> hits = TargetScanner.hits();
        List<Sus> sus = SusChunkScanner.found();
        boolean playersOn = VerdantConfig.PLAYERS.enabled && VerdantConfig.PLAYER_TARGET.enabled;
        if (hits.isEmpty() && sus.isEmpty() && !playersOn) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = context.worldState().cameraRenderState.pos;
        Matrix4fc pose = context.matrices().last().pose();
        Vec3 look = mc.player.getLookAngle();
        double seconds = System.nanoTime() / 1.0e9;

        int index = 0;
        for (Hit hit : hits) {
            Module module = hit.target().module;
            BoxBuffer out = module.throughWalls ? THROUGH_WALLS : DEPTH_TESTED;
            int rgb = colorFor(module, hit.target().rgb(), seconds, index++);
            float fade = fadeFor(module, hit.distance());

            drawBoxSet(out, pose, cam, module, rgb, fade, hit.distance(),
                    hit.minX(), hit.minY(), hit.minZ(), hit.maxX(), hit.maxY(), hit.maxZ());

            if (module.tracers) {
                drawTracer(out, pose, cam, look, module, rgb, fade,
                        (hit.minX() + hit.maxX()) / 2, (hit.minY() + hit.maxY()) / 2, (hit.minZ() + hit.maxZ()) / 2);
            }
        }

        Module susModule = VerdantConfig.SUS;
        for (Sus chunk : sus) {
            BoxBuffer out = susModule.throughWalls ? THROUGH_WALLS : DEPTH_TESTED;
            int rgb = colorFor(susModule, VerdantConfig.SUS_CHUNK.rgb(), seconds, index++);
            float fade = fadeFor(susModule, chunk.distance());

            double x0 = chunk.chunkX() * 16.0;
            double z0 = chunk.chunkZ() * 16.0;
            double y0 = chunk.y() + 1.0; // sits on top of the highest suspicious block
            drawBoxSet(out, pose, cam, susModule, rgb, fade, chunk.distance(),
                    x0, y0, z0, x0 + 16.0, y0 + 0.15, z0 + 16.0);

            if (susModule.tracers) {
                drawTracer(out, pose, cam, look, susModule, rgb, fade, x0 + 8.0, y0, z0 + 8.0);
            }
        }

        if (playersOn) {
            drawPlayers(mc, pose, cam, look, seconds, index);
        }

        THROUGH_WALLS.flush(mc);
        DEPTH_TESTED.flush(mc);
    }

    /** Boxes (and tracers) around other players, smoothed between game ticks. */
    private static void drawPlayers(Minecraft mc, Matrix4fc pose, Vec3 cam, Vec3 look, double seconds, int index) {
        Module module = VerdantConfig.PLAYERS;
        BoxBuffer out = module.throughWalls ? THROUGH_WALLS : DEPTH_TESTED;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (AbstractClientPlayer other : mc.level.players()) {
            if (other == mc.player || other.isSpectator() || !other.isAlive()) continue;

            double distance = mc.player.distanceTo(other);
            if (distance > module.range) continue;

            // move the box to where the player is drawn this frame
            double ix = other.xo + (other.getX() - other.xo) * partial;
            double iy = other.yo + (other.getY() - other.yo) * partial;
            double iz = other.zo + (other.getZ() - other.zo) * partial;
            AABB box = other.getBoundingBox().move(ix - other.getX(), iy - other.getY(), iz - other.getZ());

            int rgb = colorFor(module, VerdantConfig.PLAYER_TARGET.rgb(), seconds, index++);
            float fade = fadeFor(module, distance);

            drawBoxSet(out, pose, cam, module, rgb, fade, distance,
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);

            if (module.tracers) {
                drawTracer(out, pose, cam, look, module, rgb, fade,
                        (box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
            }
        }
    }

    private static int colorFor(Module module, int normalRgb, double seconds, int index) {
        if (!module.rainbow) return normalRgb;
        float hue = (float) ((seconds * 0.15 * module.rainbowSpeed) + index * 0.013);
        return VerdantConfig.hsbToRgb(hue, 0.85f, 1f);
    }

    private static float fadeFor(Module module, double distance) {
        if (!module.distanceFade) return 1f;
        return (float) Math.max(0.15, 1.0 - distance / Math.max(1.0, module.range));
    }

    private static void drawTracer(BoxBuffer out, Matrix4fc pose, Vec3 cam, Vec3 look, Module module,
                                   int rgb, float fade, double tx, double ty, double tz) {
        double ax = look.x * TRACER_START;
        double ay = look.y * TRACER_START;
        double az = look.z * TRACER_START;
        double bx = tx - cam.x;
        double by = ty - cam.y;
        double bz = tz - cam.z;
        double halfWidthPerDistance = module.tracerWidth * 0.0007;
        out.addLine(pose, ax, ay, az, bx, by, bz, halfWidthPerDistance, rgb, module.tracerOpacity * fade);
    }

    private static void drawBoxSet(BoxBuffer out, Matrix4fc pose, Vec3 cam, Module module, int rgb, float fade,
                                   double distance, double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        double grow = 0.002; // tiny gap so the box does not flicker against the block faces
        double x0 = minX - grow;
        double y0 = minY - grow;
        double z0 = minZ - grow;
        double x1 = maxX + grow;
        double y1 = maxY + grow;
        double z1 = maxZ + grow;

        if (module.fill && module.fillOpacity > 0.01f) {
            out.addBox(pose, cam.x, cam.y, cam.z, x0, y0, z0, x1, y1, z1, rgb, module.fillOpacity * fade);
        }

        if (module.outlineOpacity > 0.01f) {
            float alpha = module.outlineOpacity * fade;
            // Thickness is in blocks; it grows a little with distance so far-away outlines stay visible.
            double thickness = module.outlineWidth * 0.006 * (1.0 + distance / 32.0);
            double h = thickness / 2.0;

            // 4 edges along X (full length, including the corners)
            for (int i = 0; i < 4; i++) {
                double y = (i & 1) == 0 ? y0 : y1;
                double z = (i & 2) == 0 ? z0 : z1;
                out.addBox(pose, cam.x, cam.y, cam.z, x0 - h, y - h, z - h, x1 + h, y + h, z + h, rgb, alpha);
            }
            // 4 edges along Y (between the X edges so corners are not drawn twice); skipped on flat plates
            if (y1 - y0 > 2 * h + 0.001) {
                for (int i = 0; i < 4; i++) {
                    double x = (i & 1) == 0 ? x0 : x1;
                    double z = (i & 2) == 0 ? z0 : z1;
                    out.addBox(pose, cam.x, cam.y, cam.z, x - h, y0 + h, z - h, x + h, y1 - h, z + h, rgb, alpha);
                }
            }
            // 4 edges along Z
            for (int i = 0; i < 4; i++) {
                double x = (i & 1) == 0 ? x0 : x1;
                double y = (i & 2) == 0 ? y0 : y1;
                out.addBox(pose, cam.x, cam.y, cam.z, x - h, y - h, z0 + h, x + h, y + h, z1 - h, rgb, alpha);
            }
        }
    }
}
