package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.screen.BladePauseScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screen.GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void bladeclient$replacePauseMenu(CallbackInfo ci) {
        if (this.client == null) return;
        this.client.setScreen(new BladePauseScreen());
        ci.cancel();
    }
}
