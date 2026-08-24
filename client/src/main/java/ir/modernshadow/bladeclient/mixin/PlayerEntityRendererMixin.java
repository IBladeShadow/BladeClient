package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.util.BladeClientUsers;
import ir.modernshadow.bladeclient.util.SpectatorGhostState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private Text bladeclient$prefixNameTag(Text text, PlayerEntityRenderState state) {
        if (text == null || state == null) return text;
        if (!bladeclient$isBladeClient(state, text)) return text;
        return BladeClientUsers.withIconIfMissing(text);
    }

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private Text bladeclient$prefixNameTagEntity(Text text, EntityRenderState state) {
        if (text == null || state == null) return text;
        String name = null;
        if (state instanceof PlayerEntityRenderState playerState) {
            if (bladeclient$isBladeClient(playerState, text)) {
                return BladeClientUsers.withIconIfMissing(text);
            }
            name = playerState.name;
            if (name == null && playerState.playerName != null) {
                name = playerState.playerName.getString();
            }
        }
        if (name == null && state.displayName != null) {
            name = state.displayName.getString();
        }
        if (name == null || name.isEmpty()) {
            name = text.getString();
        }
        if (!BladeClientUsers.isBladeClientName(name)) return text;
        return BladeClientUsers.withIconIfMissing(text);
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void bladeclient$markLabel(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player == null || state == null) return;
        if (player.isSpectator()) {
            state.spectator = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean firstPersonSelf = mc != null
                    && mc.player == player
                    && mc.options != null
                    && mc.options.getPerspective().isFirstPerson();
            state.invisible = true;
            state.invisibleToPlayer = firstPersonSelf;
            if (state instanceof SpectatorGhostState ghostState) {
                ghostState.bladeclient$setSpectatorGhost(true);
            }
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player == player && player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                state.spectator = false;
                state.invisible = true;
                state.invisibleToPlayer = false;
            } else if (state instanceof SpectatorGhostState ghostState) {
                ghostState.bladeclient$setSpectatorGhost(false);
            }
        }
        if (!BladeClientUsers.isBladeClient(player.getUuid())) return;
        Text base = state.displayName;
        if (base == null) {
            base = player.getDisplayName();
        }
        if (base == null) return;
        state.displayName = BladeClientUsers.withIconIfMissing(base);
    }

    @Inject(
            method = "shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bladeclient$hideSpectatorFeatures(PlayerEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (state instanceof SpectatorGhostState ghostState && ghostState.bladeclient$isSpectatorGhost()) {
            cir.setReturnValue(false);
        }
    }

    private boolean bladeclient$isBladeClient(PlayerEntityRenderState state, Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc != null ? mc.world : null;
        if (world != null) {
            EntityLike entity = world.getEntityById(state.id);
            if (entity instanceof AbstractClientPlayerEntity player) {
                return BladeClientUsers.isBladeClient(player.getUuid());
            }
            String playerName = state.playerName != null ? state.playerName.getString() : null;
            if (playerName == null || playerName.isEmpty()) {
                playerName = state.name;
            }
            if ((playerName == null || playerName.isEmpty()) && text != null) {
                playerName = text.getString();
            }
            if (playerName != null && !playerName.isEmpty()) {
                for (AbstractClientPlayerEntity p : world.getPlayers()) {
                    if (p != null && playerName.equals(p.getName().getString())) {
                        return BladeClientUsers.isBladeClient(p.getUuid());
                    }
                }
            }
            if (text != null) {
                String rendered = text.getString();
                for (AbstractClientPlayerEntity p : world.getPlayers()) {
                    if (p == null) continue;
                    String baseName = p.getName().getString();
                    if (rendered.contains(baseName)) {
                        return BladeClientUsers.isBladeClient(p.getUuid());
                    }
                }
            }
        }
        String name = state.name;
        if ((name == null || name.isEmpty()) && state.playerName != null) {
            name = state.playerName.getString();
        }
        if ((name == null || name.isEmpty())) {
            name = text.getString();
        }
        return BladeClientUsers.isBladeClientName(name);
    }
}
