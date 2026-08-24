package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.account.AccountManager;
import ir.modernshadow.bladeclient.account.LauncherBridge;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.widget.GlassButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AccountSwitcherScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;
    private static final Identifier OFFLINE_ICON = Identifier.of("bladeclient", "textures/gui/accounts/offline.png");

    private final Screen parent;
    private TextFieldWidget addField;
    private ButtonWidget addBtn;
    private ButtonWidget microsoftBtn;
    private final List<ClickableWidget> accountWidgets = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listBottom;

    private boolean microsoftAuthActive;
    private String microsoftStatus;
    private CompletableFuture<LauncherBridge.MicrosoftSession> microsoftFuture;

    public AccountSwitcherScreen(Screen parent) {
        super(Text.literal("Account Switcher"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        accountWidgets.clear();

        AccountManager.captureLauncher(MinecraftClient.getInstance());

        panelW = Math.min(PANEL_W, this.width - 32);
        panelH = Math.min(PANEL_H, this.height - 32);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int x = panelX + 20;
        int w = panelW - 40;
        int backY = panelY + panelH - ROW_H - 12;
        int addY = backY - ROW_H - 8;

        addField = new TextFieldWidget(this.textRenderer, x, addY, w - 96, ROW_H, Text.literal("Offline name"));
        addField.setMaxLength(16);
        addField.setChangedListener(val -> updateAddState());
        addDrawableChild(addField);

        addBtn = new GlassButtonWidget(x + w - 90, addY, 90, ROW_H, Text.literal("Add Account"),
                b -> addAccount(addField.getText()), 0.95f);
        addDrawableChild(addBtn);

        int msY = addY - ROW_H - 8;
        String msName = AccountManager.getMicrosoftName();
        Text msLabel = msName != null ? Text.literal("Microsoft: " + msName) : Text.literal("Login with Microsoft");
        microsoftBtn = new GlassButtonWidget(x, msY, w, ROW_H, msLabel,
                b -> startMicrosoftLogin(), 0.95f);
        microsoftBtn.active = !microsoftAuthActive;
        addDrawableChild(microsoftBtn);

        ButtonWidget back = new GlassButtonWidget(x, backY, w, ROW_H, Text.literal("Back"),
                b -> MinecraftClient.getInstance().setScreen(parent), 0.95f);
        addDrawableChild(back);

        listX = x;
        listY = panelY + 64;
        listW = w;
        listBottom = addY - 8;

        refreshAccounts();
        updateAddState();
    }

    private void refreshAccounts() {
        for (ClickableWidget w : accountWidgets) remove(w);
        accountWidgets.clear();

        BladeClientConfig.Account cfg = ConfigManager.get().account;
        int y = listY;
        int delW = 24;
        int gap = 4;
        int nameW = listW - delW - gap;

        for (String raw : cfg.offlineAccounts) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty() || y + ROW_H > listBottom) continue;
            boolean active = cfg.useOffline && cfg.offlineName.equalsIgnoreCase(name);
            IconAccountButton use = new IconAccountButton(listX, y, nameW, ROW_H, name, active, OFFLINE_ICON, () -> applyAccount(name));
            ButtonWidget del = new GlassButtonWidget(listX + nameW + gap, y, delW, ROW_H, Text.literal("X"),
                    b -> removeAccount(name), 0.95f);
            addDrawableChild(use);
            addDrawableChild(del);
            accountWidgets.add(use);
            accountWidgets.add(del);
            y += ROW_H + ROW_GAP;
        }
    }

    private void updateAddState() {
        if (addBtn == null || addField == null) return;
        addBtn.active = !addField.getText().trim().isEmpty();
    }

    private void applyAccount(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) return;
        BladeClientConfig.Account cfg = ConfigManager.get().account;
        String existing = findIgnoreCase(cfg.offlineAccounts, name);
        String finalName = existing != null ? existing : name;
        if (existing == null) cfg.offlineAccounts.add(finalName);
        cfg.useOffline = true;
        cfg.offlineName = finalName;
        ConfigManager.saveQuiet();
        AccountManager.applyOffline(MinecraftClient.getInstance(), finalName);
        refreshAccounts();
    }

    private void addAccount(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) return;
        applyAccount(name);
        addField.setText("");
        updateAddState();
    }

    private void removeAccount(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) return;
        BladeClientConfig.Account cfg = ConfigManager.get().account;
        cfg.offlineAccounts.removeIf(n -> n != null && n.equalsIgnoreCase(name));
        if (cfg.offlineAccounts.isEmpty()) cfg.offlineAccounts.add("Player");
        if (cfg.offlineName.equalsIgnoreCase(name)) {
            cfg.offlineName = cfg.offlineAccounts.get(0);
            if (cfg.useOffline) AccountManager.applyOffline(MinecraftClient.getInstance(), cfg.offlineName);
        }
        ConfigManager.saveQuiet();
        refreshAccounts();
    }

    private static String sanitize(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("[^A-Za-z0-9_]", "").substring(0, Math.min(16, raw.trim().replaceAll("[^A-Za-z0-9_]", "").length()));
    }

    private static String findIgnoreCase(List<String> list, String name) {
        for (String entry : list) if (entry != null && entry.equalsIgnoreCase(name)) return entry;
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x70000000);

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x3310131B);
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF8B929E);
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF8B929E);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0x443A3F4A);
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0x443A3F4A);

        Session session = MinecraftClient.getInstance().getSession();
        String currentName = session != null ? session.getUsername() : "Unknown";
        String currentType = session != null && session.getAccountType() != null ? session.getAccountType().getName() : "Unknown";

        BladeFonts.drawUi(context, "Account Switcher", panelX + 16, panelY + 16, 0xFFFFFFFF, BladeFonts.UI_SIZE, true);
        String msName = AccountManager.getMicrosoftName();
        if (msName != null) {
            BladeFonts.drawUi(context, "Microsoft: " + msName, panelX + 16, panelY + 30, 0xFF4AA3FF, BladeFonts.UI_SMALL, true);
            BladeFonts.drawUi(context, "Current: " + currentName, panelX + 16, panelY + 42, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        } else {
            BladeFonts.drawUi(context, "Current: " + currentName, panelX + 16, panelY + 30, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        }
        BladeFonts.drawUi(context, "Type: " + currentType, panelX + 16, panelY + 44, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);

        super.render(context, mouseX, mouseY, delta);

        if (microsoftAuthActive) {
            renderMicrosoftOverlay(context);
        }
    }

    private void renderMicrosoftOverlay(DrawContext context) {
        int ox = panelX + 10;
        int oy = panelY + 60;
        int ow = panelW - 20;
        int oh = panelH - 76;

        context.fill(ox, oy, ox + ow, oy + oh, 0xCC10131B);
        context.fill(ox, oy, ox + ow, oy + 1, 0xFF4AA3FF);
        context.fill(ox, oy + oh - 1, ox + ow, oy + oh, 0x443A3F4A);
        context.fill(ox, oy, ox + 1, oy + oh, 0xFF8B929E);
        context.fill(ox + ow - 1, oy, ox + ow, oy + oh, 0x443A3F4A);

        int cy = oy + 10;
        BladeFonts.drawUi(context, "Microsoft Login", ox + 10, cy, 0xFFFFFFFF, BladeFonts.UI_SIZE, true);
        cy += 22;

        BladeFonts.drawUi(context, "A browser window has been opened by the", ox + 10, cy, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        cy += 14;
        BladeFonts.drawUi(context, "launcher. Please log in to your Microsoft", ox + 10, cy, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        cy += 14;
        BladeFonts.drawUi(context, "account to continue.", ox + 10, cy, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        cy += 18;

        if (microsoftStatus != null) {
            BladeFonts.drawUi(context, microsoftStatus, ox + 10, cy, 0xFFFFFFFF, BladeFonts.UI_SMALL, true);
            cy += 14;
        }

        BladeFonts.drawUi(context, "Close this screen to cancel", ox + 10, oy + oh - 16, 0xFF8B929E, BladeFonts.UI_SMALL, true);
    }

    private void startMicrosoftLogin() {
        if (microsoftAuthActive) return;

        if (!LauncherBridge.isAvailable()) {
            microsoftStatus = "§cMicrosoft login requires the BladeClient launcher";
            return;
        }

        microsoftAuthActive = true;
        microsoftStatus = "Opening browser for Microsoft login...";

        microsoftFuture = LauncherBridge.requestMicrosoftLogin();

        microsoftFuture.whenCompleteAsync((result, error) -> {
            if (error != null) {
                String msg = error.getMessage();
                if (msg == null || msg.isBlank()) msg = "Microsoft login failed in launcher.";
                microsoftStatus = "§c" + msg;
                return;
            }
            microsoftStatus = "Logged in as " + result.username();
            UUID uuid;
            try {
                String raw = result.uuid().replace("-", "");
                uuid = UUID.fromString(raw.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            } catch (Exception e) {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + result.username()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Session session = new Session(
                    result.username(),
                    uuid,
                    result.accessToken(),
                    Optional.empty(),
                    Optional.empty(),
                    Session.AccountType.MSA
            );
            AccountManager.applyMicrosoft(MinecraftClient.getInstance(), session);
            String msName = AccountManager.getMicrosoftName();
            if (msName != null) {
                microsoftBtn.active = false;
                microsoftBtn.setMessage(Text.literal("Microsoft: " + msName));
            }
        }, MinecraftClient.getInstance()::execute);
    }

    @Override
    public void removed() {
        microsoftAuthActive = false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class IconAccountButton extends ClickableWidget {
        private final String name;
        private final boolean active;
        private final Identifier icon;
        private final Runnable press;

        private IconAccountButton(int x, int y, int width, int height, String name, boolean active, Identifier icon, Runnable press) {
            super(x, y, width, height, Text.literal(name));
            this.name = name;
            this.active = active;
            this.icon = icon;
            this.press = press;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int bg = active ? 0x554AA3FF : (isHovered() ? 0x22FFFFFF : 0x4D000000);
            drawRoundedRect(context, x, y, w, h, 5, bg);

            if (isHovered() && !active) {
                context.fill(x, y, x + w, y + 1, 0x26FFFFFF);
                context.fill(x, y + h - 1, x + w, y + h, 0x26FFFFFF);
                context.fill(x, y, x + 1, y + h, 0x26FFFFFF);
                context.fill(x + w - 1, y, x + w, y + h, 0x26FFFFFF);
            }

            int iconSize = Math.max(14, h - 4);
            int iconX = x + 3;
            int iconY = y + (h - iconSize) / 2;
            float sx = iconSize / 64f;
            float sy = iconSize / 64f;
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.scale(sx, sy);
            int drawX = Math.round(iconX / sx);
            int drawY = Math.round(iconY / sy);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, drawX, drawY, 0f, 0f, 64, 64, 64, 64);
            matrices.popMatrix();

            String label = active ? name + " (active)" : name;
            BladeFonts.drawUi(context, label, x + iconSize + 8, y + (h - BladeFonts.UI_SMALL) / 2.0f + 1, 0xFFFFFFFF, BladeFonts.UI_SMALL, true);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            press.run();
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        private static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
            int radius = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
            if (radius == 0) {
                ctx.fill(x, y, x + w, y + h, color);
                return;
            }
            ctx.fill(x + radius, y, x + w - radius, y + h, color);
            ctx.fill(x, y + radius, x + radius, y + h - radius, color);
            ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
            int r2 = radius * radius;
            for (int dy = 0; dy < radius; dy++) {
                int dx = (int) Math.floor(Math.sqrt(r2 - (dy * dy)));
                int yTop = y + radius - dy - 1;
                int yBot = y + h - radius + dy;
                ctx.fill(x + radius - dx, yTop, x + radius, yTop + 1, color);
                ctx.fill(x + w - radius, yTop, x + w - radius + dx, yTop + 1, color);
                ctx.fill(x + radius - dx, yBot, x + radius, yBot + 1, color);
                ctx.fill(x + w - radius, yBot, x + w - radius + dx, yBot + 1, color);
            }
        }
    }
}
