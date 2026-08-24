package ir.modernshadow.bladeclient.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class BlockOverlayModule {
    private BlockOverlayModule() {}

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outlineContext) -> {
            BladeClientConfig cfg = ConfigManager.get();
            if (!cfg.blockOverlay.enabled) {
                return true; // use vanilla outline
            }

            BlockState state = outlineContext.blockState();
            BlockPos pos = outlineContext.blockPos();
            if (state == null || pos == null || context.world() == null) {
                return true;
            }

            VoxelShape shape = state.getOutlineShape(context.world(), pos, ShapeContext.of(outlineContext.entity()));
            if (shape.isEmpty()) {
                return true;
            }

            float[] rgb = hsvToRgb(cfg.blockOverlay.hue, cfg.blockOverlay.saturation, cfg.blockOverlay.value);
            float r = rgb[0];
            float g = rgb[1];
            float b = rgb[2];
            float a = clamp01(cfg.blockOverlay.alpha / 255.0f);
            float thickness = Math.max(0.5f, cfg.blockOverlay.thickness);

            double x = pos.getX() - outlineContext.cameraX();
            double y = pos.getY() - outlineContext.cameraY();
            double z = pos.getZ() - outlineContext.cameraZ();

            RenderSystem.lineWidth(thickness);
            DebugRenderer.drawVoxelShapeOutlines(
                    context.matrixStack(),
                    outlineContext.vertexConsumer(),
                    shape,
                    x, y, z,
                    r, g, b, a,
                    true
            );
            RenderSystem.lineWidth(1.0f);
            if (cfg.blockOverlay.fill && cfg.blockOverlay.fillAlpha > 0) {
                float fa = clamp01(cfg.blockOverlay.fillAlpha / 255.0f);
                float[] fillRgb = hsvToRgb(cfg.blockOverlay.fillHue, cfg.blockOverlay.fillSaturation, cfg.blockOverlay.fillValue);
                drawFilledShape(context.matrixStack(), context.consumers(),
                        shape, x, y, z, fillRgb[0], fillRgb[1], fillRgb[2], fa);
            }
            return false; // cancel vanilla outline
        });
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    private static float[] hsvToRgb(int hue, int saturation, int value) {
        float h = ((hue % 360) + 360) % 360;
        float s = clamp01(saturation / 100.0f);
        float v = clamp01(value / 100.0f);

        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r1, g1, b1;
        if (h < 60) {
            r1 = c; g1 = x; b1 = 0;
        } else if (h < 120) {
            r1 = x; g1 = c; b1 = 0;
        } else if (h < 180) {
            r1 = 0; g1 = c; b1 = x;
        } else if (h < 240) {
            r1 = 0; g1 = x; b1 = c;
        } else if (h < 300) {
            r1 = x; g1 = 0; b1 = c;
        } else {
            r1 = c; g1 = 0; b1 = x;
        }

        return new float[]{r1 + m, g1 + m, b1 + m};
    }

    private static void drawFilledShape(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, VoxelShape shape,
                                        double x, double y, double z, float r, float g, float b, float a) {
        for (Box box : shape.getBoundingBoxes()) {
            double minX = box.minX + x;
            double minY = box.minY + y;
            double minZ = box.minZ + z;
            double maxX = box.maxX + x;
            double maxY = box.maxY + y;
            double maxZ = box.maxZ + z;
            DebugRenderer.drawBox(matrices, consumers, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        }
    }
}
