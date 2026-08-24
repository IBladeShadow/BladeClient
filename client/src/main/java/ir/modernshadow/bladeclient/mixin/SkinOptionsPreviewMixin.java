package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class SkinOptionsPreviewMixin {
    @Inject(method = "addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;",
            at = @At("HEAD"), cancellable = true)
    private void bladeclient$skipSkinPreview(Element drawable, CallbackInfoReturnable<Element> cir) {
        if (!(((Object) this) instanceof SkinOptionsScreen)) {
            return;
        }
        if (drawable instanceof PlayerSkinWidget) {
            cir.setReturnValue(drawable);
            cir.cancel();
        }
    }
}
