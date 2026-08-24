package ir.modernshadow.bladeclient.visual;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public final class TimeChangerModule {
    private TimeChangerModule() {}

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(TimeChangerModule::tick);
        WorldRenderEvents.START.register(context -> tick(MinecraftClient.getInstance()));
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.world == null) {
            return;
        }
        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.timeChanger.enabled) {
            return;
        }
        int time = clampTime(cfg.timeChanger.time);
        client.world.getLevelProperties().setTimeOfDay(time);
    }

    private static int clampTime(int time) {
        if (time < 0) return 0;
        if (time > 23999) return 23999;
        return time;
    }
}
