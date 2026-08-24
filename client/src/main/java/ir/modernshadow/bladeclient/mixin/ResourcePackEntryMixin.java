package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.screen.PackDragState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackListWidget.ResourcePackEntry.class)
public class ResourcePackEntryMixin {
    private static final float HOVER_SPEED = 1.0f;
    private static final int MAX_ALPHA = 0x4D;
    private float hoverProgress = 0f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void bladeclient$skipRenderWhenDragging(
            DrawContext ctx,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float delta,
            CallbackInfo ci
    ) {
        if (PackDragState.isDraggingEntry(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bladeclient$hoverOverlay(
            DrawContext ctx,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float delta,
            CallbackInfo ci
    ) {
        if (PackDragState.isDraggingEntry(this)) {
            return;
        }
        if (hovered) {
            hoverProgress = Math.min(1f, hoverProgress + delta * HOVER_SPEED);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - delta * HOVER_SPEED);
        }

        int a = Math.round(MAX_ALPHA * hoverProgress);
        if (a > 0) {
            int overlay = (a << 24) | 0x00FFFFFF;
            ctx.fill(x, y, x + entryWidth, y + entryHeight, overlay);
        }
    }
}
