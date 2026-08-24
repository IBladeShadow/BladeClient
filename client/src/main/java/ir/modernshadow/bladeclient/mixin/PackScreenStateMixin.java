package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resource.ResourcePackManager;
import java.nio.file.Path;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.Screen;

@Mixin(PackScreen.class)
public abstract class PackScreenStateMixin {
    @Unique
    private Screen bladeclient$parent;


    @Unique
    private List<String> bladeclient$initialEnabled;
    @Unique
    private boolean bladeclient$skipApply;

    @Inject(method = "init", at = @At("TAIL"))
    private void bladeclient$captureInitial(CallbackInfo ci) {
        PackScreenAccessor accessor = (PackScreenAccessor) (Object) this;
        ResourcePackOrganizer organizer = accessor.bladeclient$getOrganizer();
        bladeclient$initialEnabled = getEnabledNames(organizer);
        bladeclient$skipApply = false;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bladeclient$captureParent(ResourcePackManager manager, Consumer<ResourcePackManager> applier, Path path, Text title, CallbackInfo ci) {
        bladeclient$parent = MinecraftClient.getInstance().currentScreen;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void bladeclient$skipApplyWhenUnchanged(CallbackInfo ci) {
        if (bladeclient$initialEnabled == null) {
            return;
        }
        PackScreenAccessor accessor = (PackScreenAccessor) (Object) this;
        ResourcePackOrganizer organizer = accessor.bladeclient$getOrganizer();
        List<String> currentEnabled = getEnabledNames(organizer);
        if (bladeclient$initialEnabled.equals(currentEnabled)) {
            bladeclient$skipApply = true;
        }
    }

    @Redirect(
            method = "close",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/pack/ResourcePackOrganizer;apply()V")
    )
    private void bladeclient$maybeApply(ResourcePackOrganizer organizer) {
        if (!bladeclient$skipApply) {
            organizer.apply();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void bladeclient$ensureClose(CallbackInfo ci) {
        if (!bladeclient$skipApply) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen != (Object) this) {
            return;
        }
        if (bladeclient$parent != null) {
            client.setScreen(bladeclient$parent);
        } else {
            client.setScreen(null);
        }
    }

    @Unique
    private static List<String> getEnabledNames(ResourcePackOrganizer organizer) {
        return organizer.getEnabledPacks()
                .map(ResourcePackOrganizer.Pack::getName)
                .toList();
    }
}
