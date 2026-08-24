package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.screen.BladeTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void bladeclient$replaceTitle(CallbackInfo ci) {
        if (!ConfigManager.get().ui.replaceTitleScreen) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (client.currentScreen instanceof BladeTitleScreen) return;
        client.setScreen(new BladeTitleScreen());
        ci.cancel();
    }
}
