package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHud {
    private ArmorHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(ArmorHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.armor.enabled) return;

        float scale = Math.max(0.5f, Math.min(3.0f, cfg.armor.scale));
        int x = cfg.armor.x;
        int y = cfg.armor.y;

        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(mc.player.getEquippedStack(EquipmentSlot.HEAD));
        stacks.add(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        stacks.add(mc.player.getEquippedStack(EquipmentSlot.LEGS));
        stacks.add(mc.player.getEquippedStack(EquipmentSlot.FEET));
        stacks.add(mc.player.getMainHandStack());

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);

        int drawX = Math.round(x / scale);
        int drawY = Math.round(y / scale);
        int slot = 18;
        boolean vertical = cfg.armor.layout == BladeClientConfig.ArmorLayout.VERTICAL;
        boolean showBackground = cfg.armor.showBackground != null ? cfg.armor.showBackground : true;

        for (ItemStack stack : stacks) {
            if (showBackground) {
                ctx.fill(drawX - 1, drawY - 1, drawX + slot, drawY + slot, 0x66000000);
            }
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, drawX, drawY);
                ctx.drawStackOverlay(mc.textRenderer, stack, drawX, drawY);

                if (cfg.armor.showDurability && stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int dmg = stack.getDamage();
                    int pct = Math.max(0, Math.min(100, Math.round(100f * (1f - (dmg / (float) max)))));
                    ctx.drawCenteredTextWithShadow(mc.textRenderer, RenderUtil.mcText(pct + "%"), drawX + 8, drawY + 11, 0xFFFFFFFF);
                }
            }

            if (vertical) {
                drawY += slot + 2;
            } else {
                drawX += slot + 2;
            }
        }

        matrices.popMatrix();
    }
}
