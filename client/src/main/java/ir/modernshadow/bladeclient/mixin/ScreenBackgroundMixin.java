package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.screen.AccountSwitcherScreen;
import ir.modernshadow.bladeclient.screen.BladeClientMenuScreen;
import ir.modernshadow.bladeclient.screen.BladeTitleScreen;
import ir.modernshadow.bladeclient.screen.HudEditorScreen;
import ir.modernshadow.bladeclient.screen.ModuleSettingsScreen;
import ir.modernshadow.bladeclient.screen.PackDragState;
import ir.modernshadow.bladeclient.screen.SkinManagerScreen;
import ir.modernshadow.bladeclient.screen.UiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void bladeclient$renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (MinecraftClient.getInstance().world != null) {
            // In-game menus: keep world visible (no blur, no dark filter)
            ci.cancel();
            return;
        }
        if (!shouldTheme(self)) return;
        UiTheme.drawBackground(ctx, self.width, self.height);
        ci.cancel();
    }

    private boolean shouldTheme(Screen screen) {
        // Exclude custom screens that already draw their own themed background
        if (screen instanceof BladeTitleScreen
                || screen instanceof BladeClientMenuScreen
                || screen instanceof ModuleSettingsScreen
                || screen instanceof SkinManagerScreen
                || screen instanceof HudEditorScreen
                || screen instanceof AccountSwitcherScreen) {
            return false;
        }

        if (screen instanceof HandledScreen<?> || screen instanceof ChatScreen || screen instanceof DeathScreen) {
            return false;
        }
        return true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bladeclient$renderPackDragOverlay(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof PackScreen screen)) {
            return;
        }

        PackDragState.updateDrag(screen, mouseX, mouseY);
        PackDragState.DragInfo info = PackDragState.getDragInfo(screen);
        if (info == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackOrganizer.Pack pack = info.pack();
        Text name = pack.getDisplayName();
        Text description = pack.getDecoratedDescription();
        int textWidth = client.textRenderer.getWidth(name);
        int textHeight = client.textRenderer.fontHeight;

        int iconSize = 32;
        int padding = 6;
        int gap = 6;

        int boxWidth = info.width();
        int boxHeight = info.height();
        int minWidth = iconSize + gap + textWidth + padding * 2;
        int minHeight = Math.max(iconSize, textHeight * 2) + padding * 2;
        if (boxWidth < minWidth) boxWidth = minWidth;
        if (boxHeight < minHeight) boxHeight = minHeight;

        int x = (int) Math.round(mouseX - info.offsetX());
        int y = (int) Math.round(mouseY - info.offsetY());
        int maxX = self.width - boxWidth;
        int maxY = self.height - boxHeight;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        ctx.enableScissor(0, 0, self.width, self.height);
        ctx.fill(x, y, x + boxWidth, y + boxHeight, 0xAA000000);

        int iconX = x + padding;
        int iconY = y + (boxHeight - iconSize) / 2;
        Identifier iconId = pack.getIconId();
        if (iconId != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, iconId, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, 32, 32);
        }

        int textX = iconX + iconSize + gap;
        int textY = y + padding;
        ctx.drawTextWithShadow(client.textRenderer, name, textX, textY, 0xFFFFFF);

        int maxTextWidth = Math.max(0, boxWidth - (textX - x) - padding);
        var descLines = client.textRenderer.wrapLines(description, maxTextWidth);
        if (!descLines.isEmpty()) {
            int descY = textY + textHeight + 2;
            ctx.drawTextWithShadow(client.textRenderer, descLines.get(0), textX, descY, 0xFFB0B0B0);
        }
        ctx.disableScissor();
    }

    // kept for compatibility with older checks
    private boolean isGameMenuScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.equals("net.minecraft.client.gui.screen.GameMenuScreen")
                || name.endsWith(".GameMenuScreen");
    }
}
