package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.util.SpectatorGhostState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements SpectatorGhostState {
    @Unique
    private boolean bladeclient$spectatorGhost;

    @Override
    public boolean bladeclient$isSpectatorGhost() {
        return bladeclient$spectatorGhost;
    }

    @Override
    public void bladeclient$setSpectatorGhost(boolean ghost) {
        bladeclient$spectatorGhost = ghost;
    }
}
