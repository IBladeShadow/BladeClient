package ir.modernshadow.bladeclient.chat;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AutoTextModule {
    private AutoTextModule() {}

    private static final List<EntryState> states = new ArrayList<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTextModule::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (client == null) return;
        BladeClientConfig cfg = ConfigManager.get();

        if (!cfg.autoText.enabled) {
            resetStates();
            return;
        }

        if (client.player == null || client.world == null) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        List<BladeClientConfig.AutoTextEntry> entries = cfg.autoText.entries;
        if (entries == null || entries.isEmpty()) {
            resetStates();
            return;
        }

        ensureStateSize(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            BladeClientConfig.AutoTextEntry entry = entries.get(i);
            if (entry == null) continue;

            EntryState state = states.get(i);
            String message = entry.message == null ? "" : entry.message.trim();
            state.ticks = 0;

            boolean down = isKeyDown(client, entry);
            if (down && !state.wasKeyDown && !message.isEmpty()) {
                sendMessage(client, message);
            }
            state.wasKeyDown = down;
        }
    }

    private static void sendMessage(MinecraftClient client, String message) {
        if (client.player == null) return;
        if (message.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(message);
        }
    }

    private static boolean isKeyDown(MinecraftClient client, BladeClientConfig.AutoTextEntry entry) {
        if (entry.keyCode < 0) return false;
        InputUtil.Type type = typeFrom(entry.keyType);
        InputUtil.Key key = type.createFromCode(entry.keyCode);
        if (key == InputUtil.UNKNOWN_KEY) return false;
        long window = client.getWindow().getHandle();
        if (type == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, key.getCode());
    }

    private static InputUtil.Type typeFrom(int ordinal) {
        if (ordinal == InputUtil.Type.MOUSE.ordinal()) return InputUtil.Type.MOUSE;
        return InputUtil.Type.KEYSYM;
    }

    private static void ensureStateSize(int size) {
        while (states.size() < size) {
            states.add(new EntryState());
        }
        while (states.size() > size) {
            states.remove(states.size() - 1);
        }
    }

    private static void resetStates() {
        for (EntryState state : states) {
            state.ticks = 0;
            state.wasKeyDown = false;
        }
    }

    private static final class EntryState {
        int ticks = 0;
        boolean wasKeyDown = false;
    }
}
