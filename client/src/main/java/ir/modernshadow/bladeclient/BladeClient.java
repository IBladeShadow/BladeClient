package ir.modernshadow.bladeclient;

import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.account.AccountManager;
import ir.modernshadow.bladeclient.screen.BladeCrackScreen;
import ir.modernshadow.bladeclient.chat.AutoTextModule;
import ir.modernshadow.bladeclient.hud.ArmorHud;
import ir.modernshadow.bladeclient.hud.AppleSkinHud;
import ir.modernshadow.bladeclient.hud.CoordsHud;
import ir.modernshadow.bladeclient.hud.CpsHud;
import ir.modernshadow.bladeclient.hud.CrosshairHud;
import ir.modernshadow.bladeclient.hud.DirectionHud;
import ir.modernshadow.bladeclient.hud.FpsHud;
import ir.modernshadow.bladeclient.hud.KeystrokesHud;
import ir.modernshadow.bladeclient.hud.PingHud;
import ir.modernshadow.bladeclient.hud.PotionHud;
import ir.modernshadow.bladeclient.hud.ToggleSprintModule;
import ir.modernshadow.bladeclient.freelook.FreeLookModule;
import ir.modernshadow.bladeclient.module.ModuleManager;
import ir.modernshadow.bladeclient.net.PresenceService;
import ir.modernshadow.bladeclient.screen.BladeClientMenuScreen;
import ir.modernshadow.bladeclient.screen.BladeOnboardingScreen;
import ir.modernshadow.bladeclient.skin.SkinManager;
import ir.modernshadow.bladeclient.visual.NightVisionModule;
import ir.modernshadow.bladeclient.visual.BlockOverlayModule;
import ir.modernshadow.bladeclient.visual.TimeChangerModule;
import ir.modernshadow.bladeclient.zoom.ZoomModule;
import ir.modernshadow.bladeclient.font.FontPackManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class BladeClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding zoomKey;
    private static KeyBinding freeLookKey;
    private static boolean positionsMigrated;
    private static boolean iconSet;
    private static boolean onboardingChecked;
    private static boolean fullscreenApplied;

    private static boolean antiCrackBlocked = false;

    @Override
    public void onInitializeClient() {
        if (!AntiCrack.verify()) {
            antiCrackBlocked = true;
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (!(client.currentScreen instanceof BladeCrackScreen)) {
                    client.setScreen(new BladeCrackScreen());
                }
            });
            return;
        }

        ConfigManager.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bladeclient.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.bladeclient"
        ));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bladeclient.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.bladeclient"
        ));
        ZoomModule.register(zoomKey);

        freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bladeclient.free_look",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.bladeclient"
        ));
        FreeLookModule.register(freeLookKey);
        AutoTextModule.register();

        ModuleManager.init();

        FpsHud.register();
        PingHud.register();
        CpsHud.register();
        ArmorHud.register();
        AppleSkinHud.register();
        PotionHud.register();
        CoordsHud.register();
        DirectionHud.register();
        KeystrokesHud.register();
        ToggleSprintModule.register();
        CrosshairHud.register();
        NightVisionModule.register();
        TimeChangerModule.register();
        BlockOverlayModule.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!positionsMigrated && client.getWindow() != null) {
                positionsMigrated = true;
                ConfigManager.migratePositions(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            }
            if (!fullscreenApplied && ConfigManager.get().launchFullscreen && client.getWindow() != null && client.getWindow().getHandle() != 0) {
                fullscreenApplied = true;
                try {
                    client.options.getFullscreen().setValue(true);
                } catch (Exception e) {
                    System.err.println("[BladeClient] Failed to set fullscreen: " + e.getMessage());
                }
            }
            if (!iconSet && client.getWindow() != null && client.getWindow().getHandle() != 0) {
                iconSet = true;
                try (InputStream stream = BladeClient.class.getResourceAsStream("/assets/bladeclient/textures/gui/icon.png")) {
                    if (stream != null) {
                        BufferedImage img = ImageIO.read(stream);
                        int w = img.getWidth();
                        int h = img.getHeight();
                        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
                        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int p = pixels[y * w + x];
                                buf.put((byte) ((p >> 16) & 0xFF));
                                buf.put((byte) ((p >> 8) & 0xFF));
                                buf.put((byte) (p & 0xFF));
                                buf.put((byte) ((p >> 24) & 0xFF));
                            }
                        }
                        buf.flip();
                        try (GLFWImage.Buffer iconBuf = GLFWImage.malloc(1)) {
                            iconBuf.width(w);
                            iconBuf.height(h);
                            iconBuf.pixels(buf);
                            GLFW.glfwSetWindowIcon(client.getWindow().getHandle(), iconBuf);
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[BladeClient] Failed to set window icon: " + t.getMessage());
                }
            }
            if (openMenuKey == null) return;
            while (openMenuKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> mc.setScreen(new BladeClientMenuScreen(mc.currentScreen)));
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(SkinManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(FontPackManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AccountManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(PresenceService::tick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (onboardingChecked) return;
            onboardingChecked = true;
            if (!AccountManager.hasAnyAccount()) {
                client.setScreen(new BladeOnboardingScreen());
            }
        });
    }
}