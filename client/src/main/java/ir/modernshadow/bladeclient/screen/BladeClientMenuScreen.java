package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.module.Module;
import ir.modernshadow.bladeclient.module.ModuleCategory;
import ir.modernshadow.bladeclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BladeClientMenuScreen extends Screen {
    private static final Identifier CLOSE_ICON = Identifier.of("bladeclient", "textures/gui/close.png");
    private static final Identifier COG_ICON = Identifier.of("bladeclient", "textures/gui/ui/cog-64.png");
    private static final Identifier EDIT_ICON = Identifier.of("bladeclient", "textures/gui/edit.png");
    private static final Identifier USER_ICON = Identifier.of("bladeclient", "textures/gui/user.png");
    private static final Identifier SEARCH_ICON = Identifier.of("bladeclient", "textures/gui/search.png");

    private static final int BASE_PANEL_MARGIN_X = 70;
    private static final int BASE_PANEL_MARGIN_Y = 30;
    private static final int BASE_SIDEBAR_W = 44;
    private static final int BASE_HEADER_H = 40;
    private static final int BASE_TAB_BAR_H = 28;
    private static final int BASE_SEARCH_H = 22;
    private static final int BASE_CARD_W = 150;
    private static final int BASE_CARD_H = 100;
    private static final int BASE_CARD_GAP = 8;
    private static final int BASE_MODULE_ICON_SIZE = 30;
    private static final int COLUMNS_COUNT = 3;

    private static final float BASE_TOTAL_WIDTH =
            2 * BASE_PANEL_MARGIN_X + BASE_SIDEBAR_W + 24 + 36 +
                    COLUMNS_COUNT * BASE_CARD_W + (COLUMNS_COUNT - 1) * BASE_CARD_GAP;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 1.2f;

    private static final int MENU_PANEL_BG = 0x3310131B;
    private static final int MENU_ACCENT = 0xFF8B929E;
    private static final int TAB_BG = 0x551F2430;
    private static final int TAB_HOVER_BG = 0x553A3F4A;
    private static final int TAB_SELECTED_BG = 0x55606570;
    private static final int MODULE_CARD_BG = 0x4D000000;
    private static final int MODULE_CARD_HOVER = 0x22FFFFFF;
    private static final int MODULE_CARD_BORDER = 0x26FFFFFF;
    private static final int SEARCH_BG = 0x881F2430;
    private static final int SEARCH_BORDER = 0x443A3F4A;
    private static final int SEARCH_FOCUSED_BORDER = 0x88606570;
    private static final int SIDEBAR_BOX_BG = 0x331F2430;
    private static final int SIDEBAR_BOX_BORDER = 0x443A3F4A;

    private final Screen parent;
    private final List<ModuleCardWidget> cards = new ArrayList<>();
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final List<SidebarButton> sidebarButtons = new ArrayList<>();

    private SearchFieldWidget searchField;
    private CloseButton closeButton;

    private ModuleCategory selectedCategory = ModuleCategory.ALL;

    private float scrollY = 0.0f;
    private float targetScrollY = 0.0f;
    private int maxScroll = 0;

    private boolean draggingScrollBar = false;
    private int scrollBarDragOffset = 0;

    private boolean skipCardRender = false;
    private int lastW = -1;
    private int lastH = -1;

    private ModuleSettingsScreen embeddedSettings;
    private int dynamicCardGap = BASE_CARD_GAP;

    private float sidebarAnimProgress = 0.0f;
    private boolean sidebarHovered = false;
    private static final float SIDEBAR_ANIM_SPEED = 0.15f;

    private int sidebarBoxX, sidebarBoxY, sidebarBoxW, sidebarBoxH;

    public BladeClientMenuScreen(Screen parent) {
        super(Text.literal("BladeClient"));
        this.parent = parent;
    }

    private float uiScale() {
        float scale = this.width / BASE_TOTAL_WIDTH;
        return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
    }

    private int s(int value) {
        return Math.round(value * uiScale());
    }

    private Layout layout() {
        float scale = uiScale();
        int panelMarginX = s(BASE_PANEL_MARGIN_X);
        int panelMarginY = s(BASE_PANEL_MARGIN_Y);
        int sidebarW = s(BASE_SIDEBAR_W);
        int headerH = s(BASE_HEADER_H);
        int tabBarH = s(BASE_TAB_BAR_H);
        int searchH = s(BASE_SEARCH_H);
        int cardW = s(BASE_CARD_W);
        int cardH = s(BASE_CARD_H);
        int cardGap = s(BASE_CARD_GAP);
        int moduleIconSize = s(BASE_MODULE_ICON_SIZE);

        int panelX = panelMarginX;
        int panelY = panelMarginY;
        int panelW = this.width - panelMarginX * 2;
        int panelH = this.height - panelMarginY * 2;

        int gridX = panelX + sidebarW + s(12);
        int gridY = panelY + headerH + searchH + tabBarH + s(16);
        int gridW = panelW - sidebarW - s(24);
        int gridH = panelH - headerH - searchH - tabBarH - s(28);

        return new Layout(panelX, panelY, panelW, panelH,
                sidebarW, headerH, searchH, tabBarH,
                cardW, cardH, cardGap, moduleIconSize,
                gridX, gridY, gridW, gridH);
    }

    @Override
    protected void init() {
        clearChildren();
        cards.clear();
        tabButtons.clear();
        sidebarButtons.clear();
        lastW = this.width;
        lastH = this.height;

        Layout l = layout();

        // دکمه بستن
        int closeSize = s(14);
        int closeX = l.panelX + l.panelW - closeSize - s(10);
        int closeY = l.panelY + s(10);
        closeButton = new CloseButton(closeX, closeY, closeSize, closeSize,
                () -> MinecraftClient.getInstance().setScreen(parent));
        addDrawableChild(closeButton);

        // محاسبه موقعیت کادر سایدبار
        int sidebarIconSize = s(22);
        int sidebarGap = s(8);
        sidebarBoxX = l.panelX + s(4);
        sidebarBoxY = l.panelY + l.panelH - sidebarIconSize * 2 - sidebarGap - s(16);
        sidebarBoxW = l.sidebarW - s(4);
        sidebarBoxH = sidebarIconSize * 2 + sidebarGap + s(16);

        // دکمه‌های سایدبار (پایین داخل کادر)
        int sidebarX = l.panelX + s(11);
        int sidebarStartY = sidebarBoxY + s(8);

        SidebarButton hudBtn = new SidebarButton(sidebarX, sidebarStartY, sidebarIconSize, sidebarIconSize,
                EDIT_ICON, "HUD Editor",
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(this)));
        addDrawableChild(hudBtn);
        sidebarButtons.add(hudBtn);

        SidebarButton skinBtn = new SidebarButton(sidebarX, sidebarStartY + sidebarIconSize + sidebarGap,
                sidebarIconSize, sidebarIconSize,
                USER_ICON, "Skins",
                () -> MinecraftClient.getInstance().setScreen(new SkinManagerScreen(this)));
        addDrawableChild(skinBtn);
        sidebarButtons.add(skinBtn);

        // سرچ بار سفارشی
        int searchW = l.panelW - l.sidebarW - s(60);
        int searchX = l.panelX + l.sidebarW + s(12);
        int searchY = l.panelY + l.headerH + s(4);
        int searchH = l.searchH;
        searchField = new SearchFieldWidget(searchX, searchY, searchW, searchH, this);
        searchField.setChangedListener(text -> {
            scrollY = 0;
            targetScrollY = 0;
            applyFilter(l);
        });
        addDrawableChild(searchField);

        // زبانه‌های دسته‌بندی (زیر سرچ بار)
        int tabY = l.panelY + l.headerH + l.searchH + s(8);
        int tabHeight = l.tabBarH;
        int tabSpacing = s(4);
        int totalTabs = ModuleCategory.values().length;
        int tabWidth = Math.min((l.panelW - l.sidebarW - s(50) - tabSpacing * (totalTabs - 1)) / totalTabs, s(75));
        int tabStartX = l.panelX + l.sidebarW + s(12);

        for (ModuleCategory category : ModuleCategory.values()) {
            int idx = category.ordinal();
            int x = tabStartX + idx * (tabWidth + tabSpacing);
            TabButton btn = new TabButton(x, tabY, tabWidth, tabHeight, category,
                    () -> {
                        selectedCategory = category;
                        refreshTabs();
                        scrollY = 0;
                        targetScrollY = 0;
                        applyFilter(l);
                    });
            addDrawableChild(btn);
            tabButtons.add(btn);
        }

        // کارت‌های ماژول
        for (Module module : ModuleManager.all()) {
            if ("fonts".equals(module.id())) continue;
            ModuleCardWidget card = new ModuleCardWidget(0, 0, l.cardW, l.cardH, module, this);
            addDrawableChild(card);
            cards.add(card);
        }

        refreshTabs();
        applyFilter(l);
        searchField.setFocused(true);
    }

    private void refreshTabs() {
        for (TabButton btn : tabButtons) {
            btn.setSelected(btn.category == selectedCategory);
        }
    }

    private void applyFilter(Layout l) {
        List<Module> visible = ModuleManager.filtered(selectedCategory, searchField.getText());
        Set<Module> visibleSet = new HashSet<>(visible);

        int columns = COLUMNS_COUNT;
        int totalCardWidth = columns * l.cardW;
        int remaining = l.gridW - totalCardWidth;
        if (remaining > 0) {
            dynamicCardGap = Math.max(4, remaining / (columns + 1));
            dynamicCardGap = Math.min(dynamicCardGap, l.cardGap * 3);
        } else {
            dynamicCardGap = 4;
        }

        int usedWidth = columns * l.cardW + (columns - 1) * dynamicCardGap;
        int gridStartX = l.gridX + Math.max(0, (l.gridW - usedWidth) / 2);

        int index = 0;
        for (ModuleCardWidget card : cards) {
            if (!visibleSet.contains(card.module)) {
                card.visible = false;
                card.active = false;
                continue;
            }
            int row = index / columns;
            int col = index % columns;
            int x = gridStartX + col * (l.cardW + dynamicCardGap);
            int y = l.gridY + row * (l.cardH + dynamicCardGap) - Math.round(scrollY);

            card.setX(x);
            card.setY(y);
            card.visible = true;
            card.active = true;
            index++;
        }

        int rows = (int) Math.ceil(index / (double) columns);
        int contentH = rows == 0 ? 0 : rows * (l.cardH + dynamicCardGap) - dynamicCardGap;
        maxScroll = Math.max(0, contentH - l.gridH);
        targetScrollY = Math.max(0.0f, Math.min(targetScrollY, maxScroll));
        scrollY = Math.max(0.0f, Math.min(scrollY, maxScroll));
    }

    // =============== رویدادهای ماوس ===============
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (embeddedSettings != null) return embeddedSettings.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            ScrollbarLayout sb = getScrollbarLayout();
            if (sb.hasScroll && sb.isOverThumb((int) mouseX, (int) mouseY)) {
                draggingScrollBar = true;
                scrollBarDragOffset = (int) mouseY - sb.thumbY;
                return true;
            }
            if (sb.hasScroll && sb.isOverTrack((int) mouseX, (int) mouseY)) {
                int newThumbY = (int) mouseY - sb.thumbH / 2;
                updateScrollFromThumb(sb, newThumbY, false);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (embeddedSettings != null) return embeddedSettings.mouseDragged(mouseX, mouseY, button, dx, dy);
        if (button == 0 && draggingScrollBar) {
            ScrollbarLayout sb = getScrollbarLayout();
            if (sb.hasScroll) updateScrollFromThumb(sb, (int) mouseY - scrollBarDragOffset, true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (embeddedSettings != null) return embeddedSettings.mouseReleased(mouseX, mouseY, button);
        if (button == 0) draggingScrollBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (embeddedSettings != null)
            return embeddedSettings.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        Layout l = layout();
        if (mouseX < l.gridX || mouseX > l.gridX + l.gridW || mouseY < l.gridY || mouseY > l.gridY + l.gridH)
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        targetScrollY = (float) Math.max(0.0, Math.min(maxScroll, targetScrollY - verticalAmount * 18.0));
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (embeddedSettings != null) {
            embeddedSettings.mouseMoved(mouseX, mouseY);
            return;
        }

        sidebarHovered = mouseX >= sidebarBoxX && mouseX <= sidebarBoxX + sidebarBoxW
                && mouseY >= sidebarBoxY && mouseY <= sidebarBoxY + sidebarBoxH;

        super.mouseMoved(mouseX, mouseY);
    }

    // =============== رندر ===============
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.width != lastW || this.height != lastH) {
            init();
            return;
        }
        if (embeddedSettings != null) {
            updateEmbeddedSettingsLayout();
            embeddedSettings.render(context, mouseX, mouseY, delta);
            return;
        }

        Layout l = layout();
        updateSmoothScroll();
        updateSidebarAnimation();
        applyFilter(l);

        // آپدیت موقعیت سرچ بار
        if (searchField != null) {
            int searchW = l.panelW - l.sidebarW - s(60);
            int searchH = l.searchH;
            searchField.setWidth(searchW);
            searchField.setHeight(searchH);
            searchField.setX(l.panelX + l.sidebarW + s(12));
            searchField.setY(l.panelY + l.headerH + s(4));
        }

        if (closeButton != null) {
            int closeSize = closeButton.getWidth();
            closeButton.setX(l.panelX + l.panelW - closeSize - s(10));
            closeButton.setY(l.panelY + s(10));
        }

        // پنل اصلی
        drawRoundedRect(context, l.panelX, l.panelY, l.panelW, l.panelH, s(10), MENU_PANEL_BG);

        // هدر با لوگو
        int iconSize = s(24);
        int iconX = l.panelX + s(14);
        int iconY = l.panelY + s(8);
        drawIcon(context, iconX, iconY, iconSize, iconSize);

        BladeFonts.drawUi(context, this.title.getString(),
                iconX + iconSize + s(8), l.panelY + s(12), 0xFFFFFFFF,
                BladeFonts.UI_SIZE * uiScale(), true);

        // خط جداکننده زیر هدر
        int headerBottom = l.panelY + l.headerH;
        context.fill(l.panelX + s(10), headerBottom, l.panelX + l.panelW - s(10), headerBottom + 1, 0x22FFFFFF);

        // کادر دور دکمه‌های سایدبار با انیمیشن
        float progress = sidebarAnimProgress;
        int boxAlpha = (int) (0x33 * progress);
        int borderAlpha = (int) (0x44 * progress);
        drawRoundedRect(context, sidebarBoxX, sidebarBoxY, sidebarBoxW, sidebarBoxH, s(6),
                (boxAlpha << 24) | 0x001F2430);
        drawRoundedRectBorder(context, sidebarBoxX, sidebarBoxY, sidebarBoxW, sidebarBoxH, s(6),
                (borderAlpha << 24) | 0x003A3F4A);

        // سرچ بار سفارشی
        drawSearchBar(context, l, mouseX, mouseY);

        // آپدیت انیمیشن سایدبار
        for (SidebarButton btn : sidebarButtons) {
            btn.animProgress = sidebarAnimProgress;
        }

        // منطقه کارت‌ها
        if (selectedCategory != ModuleCategory.ALL) {
            context.fill(l.gridX, l.gridY, l.gridX + l.gridW, l.gridY + l.gridH, 0x66000000);
        }

        skipCardRender = false;
        context.enableScissor(l.gridX, l.gridY, l.gridX + l.gridW, l.gridY + l.gridH);
        for (ModuleCardWidget card : cards) {
            if (card.visible) card.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();

        skipCardRender = true;
        super.render(context, mouseX, mouseY, delta);
        skipCardRender = false;

        drawScrollbar(context);
    }

    private void drawSearchBar(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.panelX + l.sidebarW + s(12);
        int y = l.panelY + l.headerH + s(4);
        int w = l.panelW - l.sidebarW - s(60);
        int h = l.searchH;

        boolean focused = searchField != null && searchField.isFocused();
        int borderColor = focused ? SEARCH_FOCUSED_BORDER : SEARCH_BORDER;

        // بکگراند شیشه‌ای
        drawRoundedRect(context, x, y, w, h, h / 2, SEARCH_BG);
        // حاشیه
        drawRoundedRectBorder(context, x, y, w, h, h / 2, borderColor);

        // آیکون جستجو
        int iconSz = s(12);
        int iconY = y + (h - iconSz) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON,
                x + s(10), iconY, 0, 0, iconSz, iconSz, iconSz, iconSz);

        // متن جستجو یا placeholder
        String text = searchField != null ? searchField.getText() : "";
        float fontSize = BladeFonts.UI_SMALL * uiScale();

        if (!text.isEmpty()) {
            // متن وارد شده توسط کاربر با فونت خودمون
            int textX = x + s(28);
            float textY = y + (h - fontSize) / 2f + 1;
            BladeFonts.drawUi(context, text, textX, textY, 0xFFFFFFFF, fontSize, true);

            // کرسر چشمک‌زن
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int textW = BladeFonts.uiWidth(text, fontSize);
                int cursorX = textX + textW + 1;
                context.fill(cursorX, y + s(4), cursorX + 1, y + h - s(4), 0xFFFFFFFF);
            }
        } else if (!focused) {
            // placeholder وقتی فوکوس نیست با فونت خودمون
            float textY = y + (h - fontSize) / 2f + 1;
            BladeFonts.drawUi(context, "Search modules...",
                    x + s(28), textY, 0x88666666, fontSize, true);
        } else {
            // کرسر وقتی فوکوسه و متن خالیه
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                context.fill(x + s(28), y + s(4), x + s(29), y + h - s(4), 0xFFFFFFFF);
            }
        }
    }

    private void updateSidebarAnimation() {
        float target = sidebarHovered ? 1.0f : 0.0f;
        float diff = target - sidebarAnimProgress;
        if (Math.abs(diff) < 0.01f) {
            sidebarAnimProgress = target;
        } else {
            sidebarAnimProgress += diff * SIDEBAR_ANIM_SPEED;
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (embeddedSettings != null) {
            updateEmbeddedSettingsLayout();
            embeddedSettings.init(client, width, height);
        }
        init();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return embeddedSettings != null ? embeddedSettings.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char chr, int modifiers) {
        return embeddedSettings != null ? embeddedSettings.charTyped(chr, modifiers) : super.charTyped(chr, modifiers);
    }

    private void openModuleSettings(Module module) {
        embeddedSettings = new ModuleSettingsScreen(this, module, () -> embeddedSettings = null);
        updateEmbeddedSettingsLayout();
        MinecraftClient mc = MinecraftClient.getInstance();
        embeddedSettings.init(mc, this.width, this.height);
    }

    private void updateEmbeddedSettingsLayout() {
        if (embeddedSettings == null) return;
        Layout l = layout();
        embeddedSettings.setEmbeddedLayout(l.panelX, l.panelY, l.panelW, l.panelH);
    }

    // =============== اسکرول‌بار ===============
    private void drawScrollbar(DrawContext context) {
        ScrollbarLayout sb = getScrollbarLayout();
        if (!sb.hasScroll) return;
        context.fill(sb.trackX, sb.trackY, sb.trackX + sb.trackW, sb.trackY + sb.trackH, 0x33101010);
        context.fill(sb.thumbX, sb.thumbY, sb.thumbX + sb.thumbW, sb.thumbY + sb.thumbH, MENU_ACCENT);
    }

    private void updateScrollFromThumb(ScrollbarLayout sb, int newThumbY, boolean immediate) {
        int thumbY = clamp(newThumbY, sb.trackY, sb.trackY + sb.trackH - sb.thumbH);
        float t = (thumbY - sb.trackY) / (float) (sb.trackH - sb.thumbH);
        targetScrollY = t * maxScroll;
        if (immediate) scrollY = targetScrollY;
        applyFilter(layout());
    }

    private ScrollbarLayout getScrollbarLayout() {
        Layout l = layout();
        int trackW = s(6);
        int trackX = l.panelX + l.panelW - trackW - s(8);
        int trackY = l.gridY;
        int trackH = l.gridH;
        if (maxScroll <= 0) return new ScrollbarLayout(false, trackX, trackY, trackW, trackH, trackX, trackY, trackW, trackH);
        int contentH = l.gridH + maxScroll;
        int thumbH = Math.max(s(24), Math.round((l.gridH / (float) contentH) * trackH));
        int thumbY = trackY + Math.round((scrollY / (float) maxScroll) * (trackH - thumbH));
        return new ScrollbarLayout(true, trackX, trackY, trackW, trackH, trackX, thumbY, trackW, thumbH);
    }

    private void updateSmoothScroll() {
        float diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.25f) scrollY = targetScrollY;
        else scrollY += diff * 0.22f;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // =============== ابزارهای ترسیم ===============
    private static void drawIcon(DrawContext ctx, int x, int y, int w, int h) {
        float sx = w / 512f, sy = h / 512f;
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale(sx, sy);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED,
                BladeLogoMask.iconId(MinecraftClient.getInstance()),
                Math.round(x / sx), Math.round(y / sy), 0, 0, 512, 512, 512, 512);
        matrices.popMatrix();
    }

    private static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        int radius = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
        if (radius == 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        int r2 = radius * radius;
        for (int dy = 0; dy < radius; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r2 - dy * dy));
            int yTop = y + radius - dy - 1, yBot = y + h - radius + dy;
            ctx.fill(x + radius - dx, yTop, x + radius, yTop + 1, color);
            ctx.fill(x + w - radius, yTop, x + w - radius + dx, yTop + 1, color);
            ctx.fill(x + radius - dx, yBot, x + radius, yBot + 1, color);
            ctx.fill(x + w - radius, yBot, x + w - radius + dx, yBot + 1, color);
        }
    }

    private static void drawRoundedRectBorder(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        int radius = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
        if (radius == 0) {
            ctx.fill(x, y, x + w, y + 1, color);
            ctx.fill(x, y + h - 1, x + w, y + h, color);
            ctx.fill(x, y, x + 1, y + h, color);
            ctx.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        int r2 = radius * radius;
        for (int dy = 0; dy < radius; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r2 - dy * dy));
            int yTop = y + radius - dy - 1, yBot = y + h - radius + dy;
            ctx.fill(x + radius - dx, yTop, x + radius, yTop + 1, color);
            ctx.fill(x + w - radius, yTop, x + w - radius + dx, yTop + 1, color);
            ctx.fill(x + radius - dx, yBot, x + radius, yBot + 1, color);
            ctx.fill(x + w - radius, yBot, x + w - radius + dx, yBot + 1, color);
        }
        ctx.fill(x + radius, y, x + w - radius, y + 1, color);
        ctx.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
    }

    // =============== کلاس‌های داخلی ===============
    private static final class Layout {
        final int panelX, panelY, panelW, panelH;
        final int sidebarW, headerH, searchH, tabBarH;
        final int cardW, cardH, cardGap;
        final int moduleIconSize;
        final int gridX, gridY, gridW, gridH;

        Layout(int panelX, int panelY, int panelW, int panelH,
               int sidebarW, int headerH, int searchH, int tabBarH,
               int cardW, int cardH, int cardGap, int moduleIconSize,
               int gridX, int gridY, int gridW, int gridH) {
            this.panelX = panelX; this.panelY = panelY; this.panelW = panelW; this.panelH = panelH;
            this.sidebarW = sidebarW; this.headerH = headerH; this.searchH = searchH; this.tabBarH = tabBarH;
            this.cardW = cardW; this.cardH = cardH; this.cardGap = cardGap;
            this.moduleIconSize = moduleIconSize;
            this.gridX = gridX; this.gridY = gridY; this.gridW = gridW; this.gridH = gridH;
        }
    }

    private static final class ScrollbarLayout {
        final boolean hasScroll;
        final int trackX, trackY, trackW, trackH, thumbX, thumbY, thumbW, thumbH;
        ScrollbarLayout(boolean hasScroll, int trackX, int trackY, int trackW, int trackH,
                        int thumbX, int thumbY, int thumbW, int thumbH) {
            this.hasScroll = hasScroll; this.trackX = trackX; this.trackY = trackY;
            this.trackW = trackW; this.trackH = trackH;
            this.thumbX = thumbX; this.thumbY = thumbY; this.thumbW = thumbW; this.thumbH = thumbH;
        }
        boolean isOverTrack(int x, int y) { return x >= trackX && x <= trackX + trackW && y >= trackY && y <= trackY + trackH; }
        boolean isOverThumb(int x, int y) { return x >= thumbX && x <= thumbX + thumbW && y >= thumbY && y <= thumbY + thumbH; }
    }

    // سرچبار سفارشی
    private static class SearchFieldWidget extends TextFieldWidget {
        public SearchFieldWidget(int x, int y, int width, int height, BladeClientMenuScreen screen) {
            super(MinecraftClient.getInstance().textRenderer, x, y, width, height, Text.literal("Search modules..."));
            this.setDrawsBackground(false);
            this.setEditableColor(0xFFFFFF);
            this.setUneditableColor(0x888888);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // رندر خالی - ما خودمون به صورت سفارشی رسمش می‌کنیم
            // اما کلیک و کیبورد همچنان کار می‌کنه
            this.setX(this.getX());
            this.setY(this.getY());
        }
    }

    // دکمه زبانه
    private static class TabButton extends ClickableWidget {
        final ModuleCategory category;
        boolean selected;
        final Runnable onPress;

        TabButton(int x, int y, int width, int height, ModuleCategory category, Runnable onPress) {
            super(x, y, width, height, Text.literal(category.label));
            this.category = category;
            this.onPress = onPress;
        }

        void setSelected(boolean selected) { this.selected = selected; }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int bg = selected ? TAB_SELECTED_BG : (isHovered() ? TAB_HOVER_BG : TAB_BG);
            drawRoundedRect(ctx, getX(), getY(), getWidth(), getHeight(), 6, bg);
            BladeFonts.drawUiCentered(ctx, getMessage().getString(),
                    getX() + getWidth() / 2f, getY() + getHeight() / 2f,
                    0xFFFFFFFF, 11, true);
        }

        @Override
        public void onClick(double mouseX, double mouseY) { onPress.run(); }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
    }

    // دکمه سایدبار با انیمیشن
    private class SidebarButton extends ClickableWidget {
        private final Identifier icon;
        private final String tooltip;
        private final Runnable onPress;
        float animProgress = 0.0f;

        SidebarButton(int x, int y, int width, int height, Identifier icon, String tooltip, Runnable onPress) {
            super(x, y, width, height, Text.literal(tooltip));
            this.icon = icon;
            this.tooltip = tooltip;
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();

            int bgAlpha = (int) (0x44 * animProgress);
            int bg = isHovered() ? (bgAlpha << 24) | 0x00FFFFFF : (bgAlpha << 24) | 0x001F2430;
            drawRoundedRect(ctx, x, y, w, h, 5, bg);

            float sx = w / 64f, sy = h / 64f;
            var matrices = ctx.getMatrices();
            matrices.pushMatrix();
            matrices.scale(sx, sy);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon,
                    Math.round(x / sx), Math.round(y / sy), 0, 0, 64, 64, 64, 64);
            matrices.popMatrix();

            if (animProgress > 0.5f || isHovered()) {
                float tooltipAlpha = Math.min(1.0f, animProgress * 2.0f);
                BladeFonts.drawUi(ctx, tooltip,
                        x + w + s(6), y + (h - BladeFonts.UI_SMALL) / 2f,
                        ((int) (0xFF * tooltipAlpha) << 24) | 0x00FFFFFF,
                        BladeFonts.UI_SMALL, true);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) { onPress.run(); }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
    }

    // کارت ماژول
    private class ModuleCardWidget extends ClickableWidget {
        private final Module module;
        private final BladeClientMenuScreen screen;

        ModuleCardWidget(int x, int y, int width, int height, Module module, BladeClientMenuScreen screen) {
            super(x, y, width, height, Text.literal(module.name()));
            this.module = module;
            this.screen = screen;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            if (screen.skipCardRender) return;

            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            float scale = screen.uiScale();

            boolean hovered = this.isHovered();
            boolean enabled = module.isEnabled();

            drawRoundedRect(ctx, x, y, w, h, 8, MODULE_CARD_BORDER);
            drawRoundedRect(ctx, x + 1, y + 1, w - 2, h - 2, 7, MODULE_CARD_BG);
            if (hovered) drawRoundedRect(ctx, x + 1, y + 1, w - 2, h - 2, 7, MODULE_CARD_HOVER);

            int iconSize = Math.round(BASE_MODULE_ICON_SIZE * scale);
            int iconX = x + (w - iconSize) / 2;
            int iconY = y + Math.round(10 * scale);
            drawModuleIcon(ctx, module.id(), iconX, iconY, iconSize, iconSize);

            BladeFonts.drawUiCentered(ctx, module.name(),
                    x + w / 2.0f,
                    iconY + iconSize + Math.round(14 * scale),
                    0xFFFFFFFF,
                    14.0f * scale,
                    true);

            int toggleH = Math.round(22 * scale);
            int settingsSize = Math.round(22 * scale);
            int buttonsY = y + h - toggleH - Math.round(10 * scale);
            int toggleX = x + Math.round(12 * scale);
            int toggleW = w - Math.round(24 * scale) - settingsSize - Math.round(8 * scale);
            int settingsX = toggleX + toggleW + Math.round(8 * scale);

            boolean toggleHovered = mouseX >= toggleX && mouseX < toggleX + toggleW &&
                    mouseY >= buttonsY && mouseY < buttonsY + toggleH;
            boolean settingsHovered = mouseX >= settingsX && mouseX < settingsX + settingsSize &&
                    mouseY >= buttonsY && mouseY < buttonsY + settingsSize;

            int toggleColor = enabled ? 0x6644B77A : 0x66B44D4D;
            drawRoundedRect(ctx, toggleX, buttonsY, toggleW, toggleH, Math.round(5 * scale), 0x55FFFFFF);
            drawRoundedRect(ctx, toggleX + 1, buttonsY + 1, toggleW - 2, toggleH - 2,
                    Math.round(4 * scale), toggleColor);
            ctx.fill(toggleX + 2, buttonsY + 2, toggleX + toggleW - 2, buttonsY + 3, 0x33FFFFFF);
            if (toggleHovered)
                drawRoundedRect(ctx, toggleX + 1, buttonsY + 1, toggleW - 2, toggleH - 2,
                        Math.round(4 * scale), 0x1FFFFFFF);
            BladeFonts.drawUiCentered(ctx, enabled ? "Enable" : "Disable",
                    toggleX + toggleW / 2.0f, buttonsY + toggleH / 2.0f,
                    0xFFFFFFFF, BladeFonts.UI_SMALL * scale, true);

            drawRoundedRect(ctx, settingsX, buttonsY, settingsSize, settingsSize,
                    Math.round(5 * scale), 0x55FFFFFF);
            drawRoundedRect(ctx, settingsX + 1, buttonsY + 1, settingsSize - 2, settingsSize - 2,
                    Math.round(4 * scale), 0x661F2430);
            ctx.fill(settingsX + 2, buttonsY + 2, settingsX + settingsSize - 2, buttonsY + 3, 0x33FFFFFF);
            if (settingsHovered)
                drawRoundedRect(ctx, settingsX + 1, buttonsY + 1, settingsSize - 2, settingsSize - 2,
                        Math.round(4 * scale), 0x22FFFFFF);
            drawTextureIcon(ctx, COG_ICON, settingsX + 3, buttonsY + 3, settingsSize - 6, settingsSize - 6);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (module.category() == ModuleCategory.SOON) return;
            float scale = screen.uiScale();
            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
            int toggleH = Math.round(22 * scale);
            int settingsSize = Math.round(22 * scale);
            int buttonsY = y + h - toggleH - Math.round(10 * scale);
            int toggleX = x + Math.round(12 * scale);
            int toggleW = w - Math.round(24 * scale) - settingsSize - Math.round(8 * scale);
            int settingsX = toggleX + toggleW + Math.round(8 * scale);
            if (mouseX >= settingsX && mouseX < settingsX + settingsSize &&
                    mouseY >= buttonsY && mouseY < buttonsY + settingsSize) {
                screen.openModuleSettings(module);
                return;
            }
            if (mouseX >= toggleX && mouseX < toggleX + toggleW &&
                    mouseY >= buttonsY && mouseY < buttonsY + toggleH) {
                module.toggle();
            }
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }

        private void drawModuleIcon(DrawContext ctx, String id, int x, int y, int w, int h) {
            Identifier tex = Identifier.of("bladeclient", "textures/gui/modules/" + id + ".png");
            drawTextureIcon(ctx, tex, x, y, w, h);
        }

        private void drawTextureIcon(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
            float sx = w / 64f, sy = h / 64f;
            var matrices = ctx.getMatrices();
            matrices.pushMatrix();
            matrices.scale(sx, sy);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex,
                    Math.round(x / sx), Math.round(y / sy), 0, 0, 64, 64, 64, 64);
            matrices.popMatrix();
        }
    }

    private static final class CloseButton extends ClickableWidget {
        private final Runnable onPress;
        CloseButton(int x, int y, int width, int height, Runnable onPress) {
            super(x, y, width, height, Text.literal("Close"));
            this.onPress = onPress;
        }
        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
            int bg = this.isHovered() ? 0x66262626 : 0x33111111;
            drawRoundedRect(ctx, x, y, w, h, 4, bg);
            float sx = w / 64f, sy = h / 64f;
            var matrices = ctx.getMatrices();
            matrices.pushMatrix();
            matrices.scale(sx, sy);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, CLOSE_ICON,
                    Math.round(x / sx), Math.round(y / sy), 0, 0, 64, 64, 64, 64);
            matrices.popMatrix();
        }
        @Override
        public void onClick(double mouseX, double mouseY) { onPress.run(); }
        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
    }
}