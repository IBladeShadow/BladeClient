package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class AppleSkinOverlay {
    private AppleSkinOverlay() {}

    private static final Identifier ICONS = Identifier.of("bladeclient", "textures/apple_skin_icons.png");
    private static final int ICON_SIZE = 9;
    private static final int ICON_TEX_SIZE = 256;

    private static final OffsetsCache OFFSETS = new OffsetsCache();

    public static void render(DrawContext ctx, PlayerEntity player, int xRight, int y) {
        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.appleSkin.enabled) return;
        if (!cfg.appleSkin.showSaturation) return;

        HungerManager hunger = player.getHungerManager();
        float saturation = hunger.getSaturationLevel();
        if (saturation <= 0.0f) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int guiTicks = mc.inGameHud.getTicks();

        // AppleSkin-style saturation overlay using its icon sheet.
        float modifiedSaturation = Math.max(0.0f, Math.min(saturation, 20.0f));
        int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0f);

        int alphaColor = argbFromRGBA(1.0f, 1.0f, 1.0f, 1.0f);
        var offsets = OFFSETS.foodBarOffsets(guiTicks, player);

        for (int i = 0; i < endSaturationBar; i++) {
            IntPoint offset = offsets[i];
            int iconX = xRight + offset.x;
            int iconY = y + offset.y;

            float effective = (modifiedSaturation / 2.0f) - i;
            int u = 0;
            if (effective >= 1.0f) u = 3 * ICON_SIZE;
            else if (effective > 0.5f) u = 2 * ICON_SIZE;
            else if (effective > 0.25f) u = 1 * ICON_SIZE;

            ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ICONS,
                iconX,
                iconY,
                u,
                0,
                ICON_SIZE,
                ICON_SIZE,
                ICON_TEX_SIZE,
                ICON_TEX_SIZE,
                alphaColor
            );
        }
    }

    private static int argbFromRGBA(float r, float g, float b, float a) {
        return (MathHelper.floor(a * 255.0f) << 24)
            | (MathHelper.floor(r * 255.0f) << 16)
            | (MathHelper.floor(g * 255.0f) << 8)
            | MathHelper.floor(b * 255.0f);
    }

    private static final class IntPoint {
        int x;
        int y;
    }

    private static final class OffsetsCache {
        private final Random random = new Random();
        private final IntPoint[] foodBarOffsets = new IntPoint[10];
        private int lastGuiTick = 0;

        private void generate(int guiTicks, PlayerEntity player) {
            HungerManager hunger = player.getHungerManager();
            float saturationLevel = hunger.getSaturationLevel();
            int foodLevel = hunger.getFoodLevel();

            boolean shouldAnimateFood = saturationLevel <= 0.0f && guiTicks % (foodLevel * 3 + 1) == 0;

            random.setSeed((long) (guiTicks * 312871));

            for (int i = 0; i < 10; i++) {
                int x = -(i * 8) - 9;
                int y = 0;
                if (shouldAnimateFood) {
                    y += random.nextInt(3) - 1;
                }
                IntPoint point = foodBarOffsets[i];
                if (point == null) {
                    point = new IntPoint();
                    foodBarOffsets[i] = point;
                }
                point.x = x;
                point.y = y;
            }
        }

        public IntPoint[] foodBarOffsets(int guiTicks, PlayerEntity player) {
            if (guiTicks != lastGuiTick) {
                generate(guiTicks, player);
                lastGuiTick = guiTicks;
            }
            return foodBarOffsets;
        }
    }
}
