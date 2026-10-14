package com.verdant.render;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Collects colored boxes during a frame and draws them all at once.
 * Built from the official Fabric docs example for rendering in the world (1.21.11).
 * One instance draws through walls, the other respects depth (hidden behind blocks).
 */
final class BoxBuffer {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private final String name;
    private final RenderPipeline pipeline;
    private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;

    BoxBuffer(String name, boolean throughWalls) {
        this.name = name;
        if (throughWalls) {
            this.pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("verdant", "pipeline/" + name))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());
        } else {
            this.pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("verdant", "pipeline/" + name))
                    .build());
        }
    }

    /**
     * Adds one box. Positions are given in world coordinates and converted
     * to camera-relative coordinates here (keeps precision far from the origin).
     */
    void addBox(Matrix4fc pose, double camX, double camY, double camZ,
                double x0, double y0, double z0, double x1, double y1, double z1,
                int rgb, float alpha) {
        if (alpha <= 0.003f) return;
        if (buffer == null) {
            buffer = new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        float minX = (float) (x0 - camX);
        float minY = (float) (y0 - camY);
        float minZ = (float) (z0 - camZ);
        float maxX = (float) (x1 - camX);
        float maxY = (float) (y1 - camY);
        float maxZ = (float) (z1 - camZ);
        float red = ((rgb >> 16) & 255) / 255f;
        float green = ((rgb >> 8) & 255) / 255f;
        float blue = (rgb & 255) / 255f;
        float a = Math.min(1f, alpha);

        // Front Face
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, a);

        // Back face
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, a);

        // Left face
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, a);

        // Right face
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, a);

        // Top face
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, a);

        // Bottom face
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, a);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, a);
    }

    /**
     * Adds a tracer line as a flat ribbon that faces the camera. Coordinates are already
     * relative to the camera. halfWidthPerDistance keeps the line about the same thickness
     * on screen at any distance, so it looks thin near the crosshair and never disappears far away.
     */
    void addLine(Matrix4fc pose, double ax, double ay, double az, double bx, double by, double bz,
                 double halfWidthPerDistance, int rgb, float alpha) {
        if (alpha <= 0.003f) return;
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double mx = (ax + bx) * 0.5;
        double my = (ay + by) * 0.5;
        double mz = (az + bz) * 0.5;
        // side direction = line direction x direction to the line's middle
        double sx = dy * mz - dz * my;
        double sy = dz * mx - dx * mz;
        double sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-9) return;
        sx /= len;
        sy /= len;
        sz /= len;
        double wa = halfWidthPerDistance * Math.sqrt(ax * ax + ay * ay + az * az);
        double wb = halfWidthPerDistance * Math.sqrt(bx * bx + by * by + bz * bz);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }
        float red = ((rgb >> 16) & 255) / 255f;
        float green = ((rgb >> 8) & 255) / 255f;
        float blue = (rgb & 255) / 255f;
        float a = Math.min(1f, alpha);

        // The ribbon is added twice with opposite winding so it is visible from both sides.
        vertex(pose, ax + sx * wa, ay + sy * wa, az + sz * wa, red, green, blue, a);
        vertex(pose, ax - sx * wa, ay - sy * wa, az - sz * wa, red, green, blue, a);
        vertex(pose, bx - sx * wb, by - sy * wb, bz - sz * wb, red, green, blue, a);
        vertex(pose, bx + sx * wb, by + sy * wb, bz + sz * wb, red, green, blue, a);

        vertex(pose, bx + sx * wb, by + sy * wb, bz + sz * wb, red, green, blue, a);
        vertex(pose, bx - sx * wb, by - sy * wb, bz - sz * wb, red, green, blue, a);
        vertex(pose, ax - sx * wa, ay - sy * wa, az - sz * wa, red, green, blue, a);
        vertex(pose, ax + sx * wa, ay + sy * wa, az + sz * wa, red, green, blue, a);
    }

    private void vertex(Matrix4fc pose, double x, double y, double z, float r, float g, float b, float a) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    /** Draws everything collected this frame. Does nothing if no boxes were added. */
    void flush(Minecraft client) {
        if (buffer == null) return;

        // Build the buffer
        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, builtBuffer, drawParameters, vertices, format);

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        // Calculate the size needed for the vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Initialize or resize the vertex buffer as needed
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }
            int size = Math.max(vertexBufferSize, 1 << 16);
            vertexBuffer = new MappableRingBuffer(() -> "verdant " + name + " vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        // Copy vertex data into the vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return vertexBuffer.currentBuffer();
    }

    private void draw(Minecraft client, MeshData builtBuffer, MeshData.DrawState drawParameters,
                      GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "verdant " + name + " render pass",
                        client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    void close() {
        allocator.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
