package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {
    @Accessor("children")
    List<?> bladeclient$getChildren();
}
