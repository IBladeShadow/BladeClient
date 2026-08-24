package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PackScreen.class)
public interface PackScreenAccessor {
    @Accessor("availablePackList")
    PackListWidget bladeclient$getAvailablePackList();

    @Accessor("selectedPackList")
    PackListWidget bladeclient$getSelectedPackList();

    @Invoker("updatePackLists")
    void bladeclient$updatePackLists();

    @Accessor("organizer")
    ResourcePackOrganizer bladeclient$getOrganizer();

    @Invoker("closeDirectoryWatcher")
    void bladeclient$closeDirectoryWatcher();
}
