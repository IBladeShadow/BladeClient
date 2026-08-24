package ir.modernshadow.bladeclient.util;

import ir.modernshadow.bladeclient.net.PresenceService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.UUID;

public final class BladeClientUsers {
    private static final Identifier ICON_FONT = Identifier.of("bladeclient", "icon");
    private BladeClientUsers() {}

    public static boolean isBladeClient(UUID uuid) {
        if (uuid == null) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && uuid.equals(client.player.getUuid())) {
            return true;
        }
        return PresenceService.hasUser(uuid);
    }

    public static boolean isBladeClientName(String name) {
        if (name == null || name.isEmpty()) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            String self = client.player.getName().getString();
            if (name.equals(self)) return true;
        }
        return PresenceService.hasUserName(name);
    }

    public static Text withIcon(Text base) {
        Text icon = Text.literal("\uE000").setStyle(Style.EMPTY.withFont(ICON_FONT).withColor(0xFF4AA3FF));
        Text space = Text.literal(" ");
        return Text.empty().append(icon).append(space).append(base);
    }

    public static boolean hasIcon(Text text) {
        if (text == null) return false;
        return text.getString().contains("\uE000");
    }

    public static Text withIconIfMissing(Text base) {
        if (base == null) return null;
        if (hasIcon(base)) return base;
        return withIcon(base);
    }
}
