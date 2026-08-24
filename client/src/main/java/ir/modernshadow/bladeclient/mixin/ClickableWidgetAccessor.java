package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor("width")
    void bladeclient$setWidth(int width);

    @Accessor("height")
    void bladeclient$setHeight(int height);
}
