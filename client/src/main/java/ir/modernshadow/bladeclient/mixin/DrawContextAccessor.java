package ir.modernshadow.bladeclient.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {
    @Invoker("drawTexturedQuad")
    void bladeclient$drawTexturedQuad(RenderPipeline pipeline, Identifier texture,
                                      int x1, int y1, int x2, int y2,
                                      float u1, float v1, float u2, float v2,
                                      int color);
}
