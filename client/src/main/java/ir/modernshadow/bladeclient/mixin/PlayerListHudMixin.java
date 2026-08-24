package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.util.BladeClientUsers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void bladeclient$prefixClientIcon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (entry == null || entry.getProfile() == null) return;
        boolean isClient = BladeClientUsers.isBladeClient(entry.getProfile().getId());
        if (!isClient && entry.getProfile().getName() != null) {
            isClient = BladeClientUsers.isBladeClientName(entry.getProfile().getName());
        }
        if (!isClient) return;
        Text base = cir.getReturnValue();
        if (base == null) return;
        cir.setReturnValue(BladeClientUsers.withIcon(base));
    }
}
