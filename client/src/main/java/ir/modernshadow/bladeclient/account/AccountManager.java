package ir.modernshadow.bladeclient.account;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class AccountManager {
    private AccountManager() {}

    private static Session launcherSession;
    private static ProfileKeys launcherProfileKeys;
    private static boolean configApplied = false;
    private static String currentMicrosoftName;

    public static void captureLauncher(MinecraftClient client) {
        if (client == null) return;
        if (launcherSession == null) {
            launcherSession = client.getSession();
            launcherProfileKeys = client.getProfileKeys();
        }
    }

    public static void applyOffline(MinecraftClient client, String username) {
        if (client == null) return;
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) return;
        captureLauncher(client);
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        Session session = new Session(name, uuid, "", Optional.empty(), Optional.empty(), Session.AccountType.LEGACY);
        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        accessor.bladeclient$setSession(session);
        accessor.bladeclient$setProfileKeys(ProfileKeys.MISSING);
    }

    public static void restoreLauncher(MinecraftClient client) {
        if (client == null || launcherSession == null) return;
        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        accessor.bladeclient$setSession(launcherSession);
        if (launcherProfileKeys != null) {
            accessor.bladeclient$setProfileKeys(launcherProfileKeys);
        }
    }

    public static Session getLauncherSession() {
        return launcherSession;
    }

    public static String getMicrosoftName() {
        return currentMicrosoftName;
    }

    public static void applyMicrosoft(MinecraftClient client, Session session) {
        if (client == null || session == null) return;
        captureLauncher(client);
        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        accessor.bladeclient$setSession(session);
        accessor.bladeclient$setProfileKeys(ProfileKeys.MISSING);
        currentMicrosoftName = session.getUsername();
    }

    public static boolean hasAnyAccount() {
        BladeClientConfig.Account cfg = ConfigManager.get().account;
        boolean hasOffline = cfg.offlineAccounts != null && !cfg.offlineAccounts.isEmpty()
                && cfg.offlineAccounts.stream().anyMatch(n -> n != null && !n.trim().isEmpty());
        return hasOffline || currentMicrosoftName != null;
    }

    public static void tick(MinecraftClient client) {
        if (configApplied) return;
        configApplied = true;
        BladeClientConfig.Account cfg = ConfigManager.get().account;
        if (cfg.useOffline) {
            String name = cfg.offlineName == null ? "" : cfg.offlineName.trim();
            if (!name.isEmpty()) {
                applyOffline(client, name);
            }
        }
    }

}
