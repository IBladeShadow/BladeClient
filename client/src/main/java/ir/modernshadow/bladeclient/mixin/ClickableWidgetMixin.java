package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.screen.AccountSwitcherScreen;
import ir.modernshadow.bladeclient.screen.BladeClientMenuScreen;
import ir.modernshadow.bladeclient.screen.BladeTitleScreen;
import ir.modernshadow.bladeclient.screen.HudEditorScreen;
import ir.modernshadow.bladeclient.screen.ModuleSettingsScreen;
import ir.modernshadow.bladeclient.screen.SkinManagerScreen;
import ir.modernshadow.bladeclient.font.BladeFonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.LockButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {
    @Unique
    private static final float HOVER_SPEED = 1.0f;

    @Unique
    private static final int MAX_ALPHA = 0x4D; // ~30%

    @Unique
    private float bladeclient$hoverProgress = 0f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void bladeclient$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ClickableWidget self = (ClickableWidget) (Object) this;
        if (!(self instanceof ButtonWidget)) return;
        if (self instanceof LockButtonWidget) return;
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!shouldTheme(screen)) return;

        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();

        int bg = 0x88222233;
        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, 0x44FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x22000000);

        boolean hovered = self.isHovered();
        if (hovered) {
            bladeclient$hoverProgress = Math.min(1f, bladeclient$hoverProgress + delta * HOVER_SPEED);
        } else {
            bladeclient$hoverProgress = Math.max(0f, bladeclient$hoverProgress - delta * HOVER_SPEED);
        }

        int a = Math.round(MAX_ALPHA * bladeclient$hoverProgress);
        int overlay = (a << 24) | 0x00FFFFFF;
        if (a > 0) context.fill(x, y, x + w, y + h, overlay);

        String label = self.getMessage().getString();
        int textCol = self.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        BladeFonts.drawUiCentered(context, label, x + w / 2.0f, y + h / 2.0f, textCol, BladeFonts.UI_SIZE, true);
        ci.cancel();
    }

    private boolean shouldTheme(Screen screen) {
        return false;
    }
}
