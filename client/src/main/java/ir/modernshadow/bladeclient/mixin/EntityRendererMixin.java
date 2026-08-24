package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.util.BladeClientUsers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Shadow
    protected void renderLabelIfPresent(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {}

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private Text bladeclient$prefixLabelEntity(Text text, EntityRenderState state) {
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

    @Inject(method = "render", at = @At("TAIL"))
    private void bladeclient$renderSelfLabel(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (state == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        String selfName = mc.player.getName().getString();
        String stateName = null;
        if (state instanceof PlayerEntityRenderState playerState) {
            stateName = playerState.name;
            if (stateName == null && playerState.playerName != null) {
                stateName = playerState.playerName.getString();
            }
        }
        if (stateName == null && state.displayName != null) {
            stateName = state.displayName.getString();
        }
        if (stateName == null || !stateName.equals(selfName)) return;

        Text label = mc.player.getDisplayName();
        if (label == null) {
            label = Text.literal(selfName);
        }
        if (state.nameLabelPos == null) {
            double y = Math.max(state.height, state.standingEyeHeight) + 0.00D;
            state.nameLabelPos = new net.minecraft.util.math.Vec3d(0.0D, y, 0.0D);
        }
        if (BladeClientUsers.isBladeClient(mc.player.getUuid())) {
            renderLabelIfPresent(state, BladeClientUsers.withIconIfMissing(label), matrices, vertexConsumers, light);
        } else {
            renderLabelIfPresent(state, label, matrices, vertexConsumers, light);
        }
    }

    private boolean bladeclient$isBladeClient(PlayerEntityRenderState state, Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            EntityLike entity = mc.world.getEntityById(state.id);
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
                for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                    if (p != null && playerName.equals(p.getName().getString())) {
                        return BladeClientUsers.isBladeClient(p.getUuid());
                    }
                }
            }
            if (text != null) {
                String rendered = text.getString();
                for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
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
