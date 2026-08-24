package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class ToggleSprintModule {
    private ToggleSprintModule() {}

    private static boolean lastSprintKeyDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ToggleSprintModule::onTick);
        HudRenderCallback.EVENT.register(ToggleSprintModule::render);
    }

    private static void onTick(MinecraftClient client) {
        if (client == null) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.toggleSprint.enabled) {
            if (cfg.toggleSprint.active) {
                cfg.toggleSprint.active = false;
                ConfigManager.saveQuiet();
            }
            lastSprintKeyDown = false;
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || player.input == null || client.options == null) return;

        if (cfg.toggleSprint.mode == BladeClientConfig.SprintMode.TOGGLE) {
            boolean sprintDown = client.options.sprintKey.isPressed();
            if (sprintDown && !lastSprintKeyDown) {
                cfg.toggleSprint.active = !cfg.toggleSprint.active;
                ConfigManager.saveQuiet();
            }
            lastSprintKeyDown = sprintDown;

            if (cfg.toggleSprint.active && canSprint(player)) {
                player.setSprinting(true);
            }
        } else if (cfg.toggleSprint.mode == BladeClientConfig.SprintMode.AUTO) {
            if (canSprint(player)) {
                player.setSprinting(true);
            }
        }
    }

    private static boolean canSprint(ClientPlayerEntity player) {
        if (player.isSneaking()) return false;
        if (player.isUsingItem()) return false;
        return player.input.hasForwardMovement();
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.toggleSprint.enabled || !cfg.toggleSprint.showHud) return;

        String label;
        int color;
        if (cfg.toggleSprint.mode == BladeClientConfig.SprintMode.AUTO) {
            label = "Auto Sprint";
            color = mc.player.isSprinting() ? 0xFF4CFF9A : 0xFFB6BDC8;
        } else {
            label = cfg.toggleSprint.active ? "Sprint: ON" : "Sprint: OFF";
            color = cfg.toggleSprint.active ? 0xFF4CFF9A : 0xFFFF6666;
        }

        float scale = RenderUtil.clamp(cfg.toggleSprint.scale, 0.5f, 3.0f);
        int x = cfg.toggleSprint.x;
        int y = cfg.toggleSprint.y;

        int textW = mc.textRenderer.getWidth(label);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);

        if (cfg.toggleSprint.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.toggleSprint.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(label), x, y, color, scale);
    }
}
