package ir.modernshadow.bladeclient.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import ir.modernshadow.bladeclient.account.AccountManager;
import ir.modernshadow.bladeclient.account.LauncherBridge;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.widget.GlassOwoButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.session.Session;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BladeTitleScreen extends BaseOwoScreen<FlowLayout> {

    private static final Text SINGLEPLAYER_TEXT = Text.translatable("menu.singleplayer");
    private static final Text MULTIPLAYER_TEXT = Text.translatable("menu.multiplayer");
    private static final Text REALMS_TEXT = Text.translatable("menu.online");
    private static final Text OPTIONS_TEXT = Text.translatable("menu.options");
    private static final Text QUIT_TEXT = Text.translatable("menu.quit");
    private static final Text ACCESSIBILITY_TEXT = Text.literal("A");
    private static final Text LANGUAGE_TEXT = Text.literal("L");
    private static final Text COPYRIGHT = Text.literal("Blade Client by IBladeShadow (1.21.8)");

    private static final Identifier MICROSOFT_ICON = Identifier.of("bladeclient", "textures/gui/accounts/microsoft.png");
    private static final Identifier OFFLINE_ICON = Identifier.of("bladeclient", "textures/gui/accounts/offline.png");

    private static final int ICON_TEX_W = 512;
    private static final int ICON_TEX_H = 512;
    private static final int ICON_DRAW_SIZE = 64;
    private static final int ICON_TEXT_PADDING = 8;
    private static final int TITLE_TEXT_Y_OFFSET = 6;

    private static final float BUTTON_TEXT_SCALE = 0.9f;

    private static final int ACCOUNT_BTN_X = 16;
    private static final int ACCOUNT_BTN_Y = 16;
    private static final int ACCOUNT_BTN_W = 110;
    private static final int ACCOUNT_BTN_H = 20;

    private static final int MENU_ITEM_W = 200;
    private static final int MENU_ITEM_H = 18;
    private static final int MENU_ROW_GAP = 4;
    private static final int MENU_PADDING = 6;
    private static final int MENU_MARGIN_BOTTOM = 16;
    private static final int MENU_MAX_VISIBLE_ROWS = 6;
    private static final int MENU_ICON_SIZE = 12;
    private static final int MENU_TEXT_X = 8 + MENU_ICON_SIZE + 6;
    private static final int MENU_DELETE_W = 16;

    private static volatile int onlinePlayers = -1;
    private static volatile long lastStatsUpdate = 0L;

    private boolean accountMenuOpen = false;

    private final List<AccountEntry> accountEntries = new ArrayList<>();

    private boolean pendingEntryClick = false;
    private double pendingClickX;
    private double pendingClickY;

    private int menuX;
    private int menuY;
    private int menuW;
    private int menuH;
    private int accountScrollOffset = 0;
    private boolean showingOfflineInput = false;
    private String offlineInputText = "";
    private boolean microsoftAuthActive = false;
    private String microsoftStatus = null;
    private CompletableFuture<LauncherBridge.MicrosoftSession> microsoftFuture;

    private GlassOwoButton accountBtn;
    private GlassOwoButton singleBtn;
    private GlassOwoButton multiBtn;
    private GlassOwoButton realmsBtn;
    private GlassOwoButton optionsBtn;
    private GlassOwoButton quitBtn;
    private GlassOwoButton langBtn;
    private GlassOwoButton accessBtn;

    public BladeTitleScreen() {
        super(Text.literal("BladeClient"));
    }

    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {

        root.surface(Surface.BLANK);
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);
        root.padding(Insets.of(0));
        root.gap(0);

        accountBtn = new GlassOwoButton(
                Text.literal("Account"),
                b -> toggleAccountMenu(),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(accountBtn);

        singleBtn = new GlassOwoButton(
                SINGLEPLAYER_TEXT,
                b -> this.client.setScreen(new SelectWorldScreen(this)),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(singleBtn);

        multiBtn = new GlassOwoButton(
                MULTIPLAYER_TEXT,
                b -> this.client.setScreen(new MultiplayerScreen(this)),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(multiBtn);

        realmsBtn = new GlassOwoButton(
                REALMS_TEXT,
                b -> this.client.setScreen(new RealmsMainScreen(this)),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(realmsBtn);

        optionsBtn = new GlassOwoButton(
                OPTIONS_TEXT,
                b -> this.client.setScreen(
                        new OptionsScreen(
                                this,
                                this.client.options
                        )
                ),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(optionsBtn);

        quitBtn = new GlassOwoButton(
                QUIT_TEXT,
                b -> this.client.scheduleStop(),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(quitBtn);

        langBtn = new GlassOwoButton(
                LANGUAGE_TEXT,
                b -> this.client.setScreen(
                        new LanguageOptionsScreen(
                                this,
                                this.client.options,
                                this.client.getLanguageManager()
                        )
                ),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(langBtn);

        accessBtn = new GlassOwoButton(
                ACCESSIBILITY_TEXT,
                b -> this.client.setScreen(
                        new AccessibilityOptionsScreen(
                                this,
                                this.client.options
                        )
                ),
                BUTTON_TEXT_SCALE,
                true
        );

        root.child(accessBtn);

        layoutButtons();
    }

    @Override
    public void init() {
        super.init();
        layoutButtons();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        layoutButtons();

        if (accountMenuOpen) {
            layoutAccountMenu();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (accountMenuOpen && button == 0) {
            pendingEntryClick = true;
            pendingClickX = mouseX;
            pendingClickY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (accountMenuOpen && isInsideMenu(mouseX, mouseY)) {
            int maxOffset = Math.max(0, accountEntries.size() - Math.min(MENU_MAX_VISIBLE_ROWS,
                    Math.max(1, (this.height - menuY - MENU_MARGIN_BOTTOM) / (MENU_ITEM_H + MENU_ROW_GAP))));
            int delta = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
            accountScrollOffset = Math.max(0, Math.min(maxOffset, accountScrollOffset + delta));
            layoutAccountMenu();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void layoutButtons() {

        int buttonWidth = 200;
        int buttonHeight = 20;
        int smallButtonWidth = 98;
        int smallIconSize = 20;

        int centerX = this.width / 2;
        int startY = this.height / 4 + 48;

        accountBtn.positioning(
                Positioning.absolute(
                        ACCOUNT_BTN_X,
                        ACCOUNT_BTN_Y
                )
        );

        accountBtn.sizing(
                Sizing.fixed(ACCOUNT_BTN_W),
                Sizing.fixed(ACCOUNT_BTN_H)
        );

        singleBtn.positioning(
                Positioning.absolute(
                        centerX - buttonWidth / 2,
                        startY
                )
        );

        singleBtn.sizing(
                Sizing.fixed(buttonWidth),
                Sizing.fixed(buttonHeight)
        );

        startY += 24;

        multiBtn.positioning(
                Positioning.absolute(
                        centerX - buttonWidth / 2,
                        startY
                )
        );

        multiBtn.sizing(
                Sizing.fixed(buttonWidth),
                Sizing.fixed(buttonHeight)
        );

        startY += 24;

        realmsBtn.positioning(
                Positioning.absolute(
                        centerX - buttonWidth / 2,
                        startY
                )
        );

        realmsBtn.sizing(
                Sizing.fixed(buttonWidth),
                Sizing.fixed(buttonHeight)
        );

        startY += 24;

        optionsBtn.positioning(
                Positioning.absolute(
                        centerX - buttonWidth / 2,
                        startY
                )
        );

        optionsBtn.sizing(
                Sizing.fixed(smallButtonWidth),
                Sizing.fixed(buttonHeight)
        );

        quitBtn.positioning(
                Positioning.absolute(
                        centerX + buttonWidth / 2 - smallButtonWidth,
                        startY
                )
        );

        quitBtn.sizing(
                Sizing.fixed(smallButtonWidth),
                Sizing.fixed(buttonHeight)
        );

        langBtn.positioning(
                Positioning.absolute(
                        this.width - smallIconSize - 24 - 2,
                        this.height - smallIconSize - 2
                )
        );

        langBtn.sizing(
                Sizing.fixed(smallIconSize),
                Sizing.fixed(smallIconSize)
        );

        accessBtn.positioning(
                Positioning.absolute(
                        this.width - smallIconSize - 2,
                        this.height - smallIconSize - 2
                )
        );

        accessBtn.sizing(
                Sizing.fixed(smallIconSize),
                Sizing.fixed(smallIconSize)
        );
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        UiTheme.drawBackground(
                context,
                this.width,
                this.height
        );

        updateOnlineCount();

        layoutButtons();

        String logoText = "BladeClient";

        float logoSize = BladeFonts.TITLE_SIZE;

        int logoWidth =
                BladeFonts.titleWidth(
                        logoText,
                        logoSize
                );

        int visualTextCenterX = this.width / 2;
        int visualTextTopY = this.height / 4;

        int iconDrawW = ICON_DRAW_SIZE;
        int iconDrawH = ICON_DRAW_SIZE;

        int iconX =
                visualTextCenterX - iconDrawW / 2;

        int iconY =
                visualTextTopY
                        - iconDrawH
                        - ICON_TEXT_PADDING;

        if (iconY < 2) iconY = 2;

        try {

            drawIcon(
                    context,
                    iconX,
                    iconY,
                    iconDrawW,
                    iconDrawH
            );

        } catch (Throwable t) {

            context.fill(
                    iconX,
                    iconY,
                    iconX + iconDrawW,
                    iconY + iconDrawH,
                    0xFF0044FF
            );
        }

        int textX =
                (int) (
                        visualTextCenterX
                                - (logoWidth / 2.0f)
                );

        int textY =
                visualTextTopY + TITLE_TEXT_Y_OFFSET;

        BladeFonts.drawTitle(
                context,
                logoText,
                textX,
                textY,
                0xFF4AA3FF,
                logoSize,
                true
        );

        float pulse =
                (float) (
                        (Math.sin(System.currentTimeMillis() / 250.0D) + 1D)
                                / 2D
                );

        int alpha =
                120 + (int) (pulse * 135);

        String onlineText;
        int onlineColor;

        if (onlinePlayers >= 0) {

            onlineText = "Online: " + onlinePlayers;

            onlineColor =
                    (alpha << 24)
                            | 0x55FF55;

        } else {

            onlineText = "Offline";

            onlineColor =
                    (alpha << 24)
                            | 0xFF5555;
        }

        int onlineWidth =
                BladeFonts.uiWidth(
                        onlineText,
                        BladeFonts.UI_SMALL
                );

        BladeFonts.drawUi(
                context,
                onlineText,
                (this.width / 2f) - (onlineWidth / 2f),
                textY + 24,
                onlineColor,
                BladeFonts.UI_SMALL,
                true
        );

        BladeFonts.drawUi(
                context,
                COPYRIGHT.getString(),
                2,
                this.height - 12,
                0xFFFFFFFF,
                BladeFonts.UI_SMALL,
                true
        );

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        if (accountMenuOpen) {

            layoutAccountMenu();

            renderAccountMenu(
                    context,
                    mouseX,
                    mouseY
            );

            if (pendingEntryClick) {
                pendingEntryClick = false;
                if (!isInsideMenu(pendingClickX, pendingClickY)) {
                    accountMenuOpen = false;
                } else {
                    for (AccountEntry entry : accountEntries) {
                        if (!isEntryVisible(entry)) continue;
                        if (entry.contains(pendingClickX, pendingClickY)) {
                            if (entry.deletePress != null && pendingClickX >= entry.x + entry.w - MENU_DELETE_W - 4 && pendingClickX <= entry.x + entry.w - 2) {
                                entry.deletePress.run();
                            } else if (entry.press != null) {
                                entry.press.run();
                            }
                            if (!entry.isActionButton && entry.press != null) {
                                accountMenuOpen = false;
                            } else {
                                rebuildAccountMenu();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void updateOnlineCount() {

        long now = System.currentTimeMillis();

        if (now - lastStatsUpdate < 3000L) {
            return;
        }

        lastStatsUpdate = now;

        Thread thread = new Thread(() -> {

            try {

                URL url =
                        new URL(
                                "https://blade.runflare.run/presence/stats"
                        );

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                try (
                        InputStreamReader reader =
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                ) {

                    JsonObject json =
                            JsonParser.parseReader(reader)
                                    .getAsJsonObject();

                    if (
                            json.has("total")
                                    && json.get("total").isJsonPrimitive()
                    ) {

                        onlinePlayers =
                                json.get("total").getAsInt();

                    } else {

                        onlinePlayers = -1;
                    }
                }

                connection.disconnect();

            } catch (Exception ignored) {

                onlinePlayers = -1;
            }

        });

        thread.setDaemon(true);
        thread.setName("BladeClient-Stats");
        thread.start();
    }

    private void drawIcon(
            DrawContext context,
            int x,
            int y,
            int w,
            int h
    ) {

        float sx = w / (float) ICON_TEX_W;
        float sy = h / (float) ICON_TEX_H;

        Matrix3x2fStack matrices =
                context.getMatrices();

        matrices.pushMatrix();

        matrices.scale(sx, sy);

        int drawX = Math.round(x / sx);
        int drawY = Math.round(y / sy);

        Identifier iconId =
                BladeLogoMask.iconId(this.client);

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                iconId,
                drawX,
                drawY,
                0.0F,
                0.0F,
                ICON_TEX_W,
                ICON_TEX_H,
                ICON_TEX_W,
                ICON_TEX_H
        );

        matrices.popMatrix();
    }

    private void toggleAccountMenu() {

        accountMenuOpen = !accountMenuOpen;

        if (accountMenuOpen) {

            accountScrollOffset = 0;
            showingOfflineInput = false;
            offlineInputText = "";

            rebuildAccountMenu();
        }
    }

    private void rebuildAccountMenu() {

        accountEntries.clear();

        String msName = AccountManager.getMicrosoftName();
        if (msName != null) {
            accountEntries.add(
                    AccountEntry.premium(
                            msName,
                            true,
                            () -> {}
                    )
            );
        }

        BladeClientConfig.Account cfg =
                ConfigManager.get().account;

        for (String raw : cfg.offlineAccounts) {

            if (raw == null) continue;

            String name = raw.trim();

            if (name.isEmpty()) continue;

            boolean active =
                    cfg.useOffline
                            && name.equalsIgnoreCase(cfg.offlineName);

            String accountName = name;
            accountEntries.add(
                    AccountEntry.offline(
                            name,
                            active,
                            () -> {
                                cfg.useOffline = true;
                                cfg.offlineName = accountName;
                                ConfigManager.saveQuiet();
                                AccountManager.applyOffline(this.client, accountName);
                            },
                            () -> removeAccount(accountName)
                    )
            );
        }

        if (accountEntries.isEmpty()) {

            accountEntries.add(
                    AccountEntry.offline(
                            "No accounts",
                            false,
                            () -> {},
                            null
                    )
            );
        }

        if (microsoftStatus != null) {
            accountEntries.add(
                    new AccountEntry(
                            microsoftStatus,
                            false,
                            null,
                            null,
                            null,
                            true
                    )
            );
        }

        if (microsoftAuthActive) {
            accountEntries.add(
                    new AccountEntry(
                            "Microsoft login in progress...",
                            false,
                            MICROSOFT_ICON,
                            null,
                            null,
                            true
                    )
            );
        } else {
            accountEntries.add(
                    new AccountEntry(
                            showingOfflineInput ? "Enter name: " + offlineInputText + "_" : "+ Add Offline",
                            false,
                            OFFLINE_ICON,
                            () -> {
                                showingOfflineInput = true;
                                offlineInputText = "";
                                rebuildAccountMenu();
                            },
                            null,
                            true
                    )
            );
            accountEntries.add(
                    new AccountEntry(
                            "Login with Microsoft",
                            false,
                            MICROSOFT_ICON,
                            () -> startMicrosoftLogin(),
                            null,
                            true
                    )
            );
        }

        layoutAccountMenu();
    }

    private void layoutAccountMenu() {

        menuX = ACCOUNT_BTN_X;
        menuY = ACCOUNT_BTN_Y + ACCOUNT_BTN_H + 6;

        int rowSpan =
                MENU_ITEM_H + MENU_ROW_GAP;

        int availableH =
                Math.max(
                        60,
                        this.height - menuY - MENU_MARGIN_BOTTOM
                );

        int maxRowsByHeight =
                Math.max(
                        1,
                        availableH / rowSpan
                );

        int maxRows =
                Math.min(
                        MENU_MAX_VISIBLE_ROWS,
                        maxRowsByHeight
                );

        int maxOffset =
                Math.max(
                        0,
                        accountEntries.size() - maxRows
                );

        accountScrollOffset =
                Math.max(
                        0,
                        Math.min(accountScrollOffset, maxOffset)
                );

        int total = accountEntries.size();

        int visible =
                Math.min(maxRows, total);

        menuW =
                MENU_ITEM_W + MENU_PADDING * 2;

        menuH =
                Math.min(
                        availableH,
                        visible * rowSpan
                                - (visible > 0 ? MENU_ROW_GAP : 0)
                                + MENU_PADDING * 2
                );

        int startX = menuX + MENU_PADDING;
        int startY = menuY + MENU_PADDING;

        for (int i = 0; i < total; i++) {

            AccountEntry entry =
                    accountEntries.get(i);

            int local =
                    i - accountScrollOffset;

            entry.x = startX;
            entry.y = startY + local * rowSpan;
            entry.w = MENU_ITEM_W;
            entry.h = MENU_ITEM_H;
        }
    }

    private void renderAccountMenu(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {

        drawRoundedRect(context, menuX, menuY, menuW, menuH, 8, 0x3310131B);
        context.fill(menuX + 8, menuY, menuX + menuW - 8, menuY + 1, 0xFF8B929E);
        context.fill(menuX, menuY + 8, menuX + 1, menuY + menuH - 8, 0xFF8B929E);
        context.fill(menuX + menuW - 1, menuY + 8, menuX + menuW, menuY + menuH - 8, 0x443A3F4A);
        context.fill(menuX + 8, menuY + menuH - 1, menuX + menuW - 8, menuY + menuH, 0x443A3F4A);

        for (AccountEntry entry : accountEntries) {

            if (!isEntryVisible(entry)) continue;

            boolean hovered = entry.isHovered(mouseX, mouseY);
            int bg;
            if (entry.active) {
                bg = 0x554AA3FF;
            } else if (hovered) {
                bg = 0x22FFFFFF;
            } else if (entry.isActionButton) {
                bg = 0x33000000;
            } else {
                bg = 0x4D000000;
            }

            drawRoundedRect(context, entry.x, entry.y, entry.w, entry.h, 5, bg);

            if (hovered && !entry.active && !entry.isActionButton) {
                context.fill(entry.x, entry.y, entry.x + entry.w, entry.y + 1, 0x26FFFFFF);
                context.fill(entry.x, entry.y + entry.h - 1, entry.x + entry.w, entry.y + entry.h, 0x26FFFFFF);
                context.fill(entry.x, entry.y, entry.x + 1, entry.y + entry.h, 0x26FFFFFF);
                context.fill(entry.x + entry.w - 1, entry.y, entry.x + entry.w, entry.y + entry.h, 0x26FFFFFF);
            }

            int iconY =
                    entry.y
                            + (entry.h - MENU_ICON_SIZE) / 2;

            if (entry.icon != null) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        entry.icon,
                        entry.x + 8,
                        iconY,
                        0.0F,
                        0.0F,
                        MENU_ICON_SIZE,
                        MENU_ICON_SIZE,
                        MENU_ICON_SIZE,
                        MENU_ICON_SIZE
                );
            }

            String label = entry.label.startsWith("§7") ? entry.label.substring(2) : entry.label;
            if (entry.active) label = label + " (active)";
            int textColor;
            if (entry.label.startsWith("§c")) {
                textColor = 0xFFFF4444;
                label = entry.label.substring(2);
            } else if (entry.label.startsWith("§a")) {
                textColor = 0xFF4CFF9A;
                label = entry.label.substring(2);
            } else if (entry.isActionButton) {
                textColor = 0xFF8B929E;
            } else {
                textColor = 0xFFFFFFFF;
            }

            int textMaxW = entry.w - MENU_TEXT_X - (entry.deletePress != null ? MENU_DELETE_W + 4 : 4);
            String displayText = label;
            if (BladeFonts.uiWidth(displayText, BladeFonts.UI_SMALL) > textMaxW) {
                while (BladeFonts.uiWidth(displayText + "...", BladeFonts.UI_SMALL) > textMaxW && displayText.length() > 1) {
                    displayText = displayText.substring(0, displayText.length() - 1);
                }
                displayText += "...";
            }

            BladeFonts.drawUi(
                    context,
                    displayText,
                    entry.x + MENU_TEXT_X,
                    entry.y + (entry.h - BladeFonts.UI_SMALL) / 2.0f + 1,
                    textColor,
                    BladeFonts.UI_SMALL,
                    true
            );

            if (entry.deletePress != null) {
                int delX = entry.x + entry.w - MENU_DELETE_W - 2;
                int delY = entry.y + (entry.h - MENU_DELETE_W) / 2;
                boolean delHover = mouseX >= delX && mouseX <= delX + MENU_DELETE_W && mouseY >= delY && mouseY <= delY + MENU_DELETE_W;
                int delBg = delHover ? 0x55FF4444 : 0x22000000;
                drawRoundedRect(context, delX, delY, MENU_DELETE_W, MENU_DELETE_W, 3, delBg);
                BladeFonts.drawUi(context, "×", delX + 4, delY + 1, delHover ? 0xFFFFFFFF : 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
            }
        }
    }

    private boolean isInsideMenu(
            double mouseX,
            double mouseY
    ) {

        return mouseX >= menuX
                && mouseX <= menuX + menuW
                && mouseY >= menuY
                && mouseY <= menuY + menuH;
    }

    private boolean isEntryVisible(AccountEntry entry) {

        int top =
                menuY + MENU_PADDING;

        int bottom =
                menuY + menuH - MENU_PADDING;

        return entry.y >= top
                && entry.y + entry.h <= bottom;
    }

    private static void drawRoundedRect(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h,
            int r,
            int color
    ) {

        int radius =
                Math.max(
                        0,
                        Math.min(
                                r,
                                Math.min(w / 2, h / 2)
                        )
                );

        if (radius == 0) {

            ctx.fill(
                    x,
                    y,
                    x + w,
                    y + h,
                    color
            );

            return;
        }

        ctx.fill(
                x + radius,
                y,
                x + w - radius,
                y + h,
                color
        );

        ctx.fill(
                x,
                y + radius,
                x + radius,
                y + h - radius,
                color
        );

        ctx.fill(
                x + w - radius,
                y + radius,
                x + w,
                y + h - radius,
                color
        );

        int r2 =
                radius * radius;

        for (int dy = 0; dy < radius; dy++) {

            int dx =
                    (int) Math.floor(
                            Math.sqrt(r2 - (dy * dy))
                    );

            int yTop =
                    y + radius - dy - 1;

            int yBot =
                    y + h - radius + dy;

            ctx.fill(
                    x + radius - dx,
                    yTop,
                    x + radius,
                    yTop + 1,
                    color
            );

            ctx.fill(
                    x + w - radius,
                    yTop,
                    x + w - radius + dx,
                    yTop + 1,
                    color
            );

            ctx.fill(
                    x + radius - dx,
                    yBot,
                    x + radius,
                    yBot + 1,
                    color
            );

            ctx.fill(
                    x + w - radius,
                    yBot,
                    x + w - radius + dx,
                    yBot + 1,
                    color
            );
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (accountMenuOpen && showingOfflineInput) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                addOfflineAccount(offlineInputText);
                showingOfflineInput = false;
                offlineInputText = "";
                rebuildAccountMenu();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                showingOfflineInput = false;
                offlineInputText = "";
                rebuildAccountMenu();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && offlineInputText.length() > 0) {
                offlineInputText = offlineInputText.substring(0, offlineInputText.length() - 1);
                rebuildAccountMenu();
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (accountMenuOpen && showingOfflineInput) {
            if (offlineInputText.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) {
                offlineInputText += chr;
                rebuildAccountMenu();
            }
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    private void addOfflineAccount(String rawName) {
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
        rebuildAccountMenu();
    }

    private void startMicrosoftLogin() {
        if (microsoftAuthActive) return;

        if (!LauncherBridge.isAvailable()) {
            microsoftStatus = "§cRequires BladeClient launcher";
            rebuildAccountMenu();
            return;
        }

        microsoftAuthActive = true;
        microsoftStatus = null;
        rebuildAccountMenu();

        microsoftFuture = LauncherBridge.requestMicrosoftLogin();
        microsoftFuture.whenCompleteAsync((result, error) -> {
            if (error != null) {
                String msg = error.getMessage();
                if (msg == null || msg.isBlank()) msg = "Microsoft login failed.";
                microsoftStatus = "§c" + msg;
                microsoftAuthActive = false;
                rebuildAccountMenu();
                return;
            }
            microsoftStatus = "§aLogged in as " + result.username();
            microsoftAuthActive = false;
            UUID uuid;
            try {
                String raw = result.uuid().replace("-", "");
                uuid = UUID.fromString(raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
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
            rebuildAccountMenu();
        }, MinecraftClient.getInstance()::execute);
    }

    private static String sanitize(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("[^A-Za-z0-9_]", "").substring(0, Math.min(16, raw.trim().replaceAll("[^A-Za-z0-9_]", "").length()));
    }

    private static String findIgnoreCase(List<String> list, String name) {
        for (String entry : list) if (entry != null && entry.equalsIgnoreCase(name)) return entry;
        return null;
    }

    private static final class AccountEntry {

        final String label;
        final boolean active;
        final Identifier icon;
        final Runnable press;
        final Runnable deletePress;
        final boolean isActionButton;

        int x;
        int y;
        int w;
        int h;

        private AccountEntry(
                String label,
                boolean active,
                Identifier icon,
                Runnable press,
                Runnable deletePress,
                boolean isActionButton
        ) {

            this.label = label;
            this.active = active;
            this.icon = icon;
            this.press = press;
            this.deletePress = deletePress;
            this.isActionButton = isActionButton;
        }

        static AccountEntry premium(
                String name,
                boolean active,
                Runnable press
        ) {

            return new AccountEntry(
                    name,
                    active,
                    MICROSOFT_ICON,
                    press,
                    null,
                    false
            );
        }

        static AccountEntry offline(
                String name,
                boolean active,
                Runnable press,
                Runnable deletePress
        ) {

            return new AccountEntry(
                    name,
                    active,
                    OFFLINE_ICON,
                    press,
                    deletePress,
                    false
            );
        }

        boolean contains(
                double mx,
                double my
        ) {

            return mx >= x
                    && mx <= x + w
                    && my >= y
                    && my <= y + h;
        }

        boolean isHovered(
                double mx,
                double my
        ) {

            return contains(mx, my);
        }
    }
}