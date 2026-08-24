package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.module.Module;
import ir.modernshadow.bladeclient.module.ModuleCategory;
import ir.modernshadow.bladeclient.module.setting.BaseSetting;
import ir.modernshadow.bladeclient.module.setting.BooleanSetting;
import ir.modernshadow.bladeclient.module.setting.EnumSetting;
import ir.modernshadow.bladeclient.module.setting.FloatSetting;
import ir.modernshadow.bladeclient.module.setting.IntSetting;
import ir.modernshadow.bladeclient.module.setting.KeybindSetting;
import ir.modernshadow.bladeclient.module.setting.StringSetting;
import ir.modernshadow.bladeclient.screen.widget.GlassButtonWidget;
import ir.modernshadow.bladeclient.screen.widget.ColorPickerWidget;
import ir.modernshadow.bladeclient.screen.widget.CrosshairEditorWidget;
import ir.modernshadow.bladeclient.screen.widget.FloatSliderWidget;
import ir.modernshadow.bladeclient.screen.widget.IntSliderWidget;
import ir.modernshadow.bladeclient.screen.widget.SettingRowButtonWidget;
import ir.modernshadow.bladeclient.screen.widget.ToggleRowWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleSettingsScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int ROW_H = 22;
    private static final int ROW_BUTTON_W = 190;
    private static final int ROW_GAP = 8;
    private static final int PICKER_H = 110;
    private static final int EMBED_PANEL_BG = 0x3310131B; // match module menu opacity
    private static final Identifier BACK_ICON = Identifier.of("bladeclient", "textures/gui/ui/left.png");
    private static final Identifier CLOSE_ICON = Identifier.of("bladeclient", "textures/gui/ui/exit-64.png");

    private final Screen parent;
    private final Module module;
    private final Runnable onBack;
    private boolean embeddedMode = false;
    private int embeddedX;
    private int embeddedY;
    private int embeddedW;
    private int embeddedH;
    private final List<ButtonWidget> rows = new ArrayList<>();
    private final List<StringRow> stringRows = new ArrayList<>();
    private final List<ScrollItem> scrollItems = new ArrayList<>();
    private ColorPickerWidget outlinePicker;
    private ColorPickerWidget fillPicker;
    private ColorPickerWidget crosshairPicker;
    private ColorSwatchRow outlineSwatch;
    private ColorSwatchRow fillSwatch;
    private ColorSwatchRow crosshairSwatch;
    private boolean miniFovAimingOpen = true;
    private boolean miniFovSprintingOpen = true;
    private boolean showOutlinePicker = false;
    private boolean showFillPicker = false;
    private boolean showCrosshairPicker = false;
    private int scrollY = 0;
    private int maxScroll = 0;
    private KeybindSetting listeningKeybind;
    private ButtonWidget listeningButton;

    public ModuleSettingsScreen(Screen parent, Module module) {
        this(parent, module, null);
    }

    public ModuleSettingsScreen(Screen parent, Module module, Runnable onBack) {
        super(Text.literal(module.name() + " Settings"));
        this.parent = parent;
        this.module = module;
        this.onBack = onBack;
    }

    public void setEmbeddedLayout(int x, int y, int w, int h) {
        this.embeddedMode = true;
        this.embeddedX = x;
        this.embeddedY = y;
        this.embeddedW = w;
        this.embeddedH = h;
    }

    @Override
    protected void init() {
        this.clearChildren();
        if ("auto_text".equals(module.id())) {
            MinecraftClient.getInstance().setScreen(new AutoTextSettingsScreen(parent));
            return;
        }
        if (module.category() == ModuleCategory.SOON) {
            MinecraftClient.getInstance().setScreen(parent);
            return;
        }
        rows.clear();
        stringRows.clear();
        scrollItems.clear();
        outlinePicker = null;
        fillPicker = null;
        crosshairPicker = null;
        outlineSwatch = null;
        fillSwatch = null;
        crosshairSwatch = null;
        Set<BaseSetting<?>> skipped = new HashSet<>();
        int panelX = embeddedMode ? embeddedX : (this.width - PANEL_W) / 2;
        int panelW = embeddedMode ? embeddedW : PANEL_W;
        int panelH = embeddedMode ? embeddedH : getPanelHeight();
        int panelY = embeddedMode ? embeddedY : (this.height - panelH) / 2;
        int contentX = panelX + 20;
        int contentW = panelW - 40;
        int y = panelY + 48;

        if (module.enabledSetting() != null) {
            int toggleW = ROW_BUTTON_W;
            int toggleX = contentX;
            ButtonWidget enabled = toggleButton(toggleX, y, toggleW, ROW_H,
                    module.enabledSetting());
            this.addDrawableChild(enabled);
            rows.add(enabled);
            scrollItems.add(new ScrollItem(enabled, y));
            y += ROW_H + ROW_GAP;
        }

        if ("blockoverlay".equals(module.id())) {
            IntSetting hue = null;
            IntSetting sat = null;
            IntSetting val = null;
            IntSetting fillHue = null;
            IntSetting fillSat = null;
            IntSetting fillVal = null;
            for (BaseSetting<?> setting : module.settings()) {
                if (setting instanceof IntSetting iset) {
                    if ("Hue".equalsIgnoreCase(iset.name())) hue = iset;
                    else if ("Saturation".equalsIgnoreCase(iset.name())) sat = iset;
                    else if ("Value".equalsIgnoreCase(iset.name())) val = iset;
                    else if ("Fill Hue".equalsIgnoreCase(iset.name())) fillHue = iset;
                    else if ("Fill Saturation".equalsIgnoreCase(iset.name())) fillSat = iset;
                    else if ("Fill Value".equalsIgnoreCase(iset.name())) fillVal = iset;
                }
            }
            if (hue != null && sat != null && val != null) {
                outlineSwatch = new ColorSwatchRow(contentX, y, contentW, ROW_H, "Outline Color", hue, sat, val, button -> {
                    showOutlinePicker = !showOutlinePicker;
                    showFillPicker = false;
                    init();
                });
                this.addDrawableChild(outlineSwatch);
                scrollItems.add(new ScrollItem(outlineSwatch, y));
                y += ROW_H + ROW_GAP;
                if (showOutlinePicker) {
                    outlinePicker = new ColorPickerWidget(contentX, y, contentW, PICKER_H, hue, sat, val);
                    this.addDrawableChild(outlinePicker);
                    scrollItems.add(new ScrollItem(outlinePicker, y));
                    y += PICKER_H + ROW_GAP;
                }
                skipped.add(hue);
                skipped.add(sat);
                skipped.add(val);
            }
            if (fillHue != null && fillSat != null && fillVal != null) {
                fillSwatch = new ColorSwatchRow(contentX, y, contentW, ROW_H, "Fill Color", fillHue, fillSat, fillVal, button -> {
                    showFillPicker = !showFillPicker;
                    showOutlinePicker = false;
                    init();
                });
                this.addDrawableChild(fillSwatch);
                scrollItems.add(new ScrollItem(fillSwatch, y));
                y += ROW_H + ROW_GAP;
                if (showFillPicker) {
                    fillPicker = new ColorPickerWidget(contentX, y, contentW, PICKER_H, fillHue, fillSat, fillVal);
                    this.addDrawableChild(fillPicker);
                    scrollItems.add(new ScrollItem(fillPicker, y));
                    y += PICKER_H + ROW_GAP;
                }
                skipped.add(fillHue);
                skipped.add(fillSat);
                skipped.add(fillVal);
            }
        }

        if ("motion_blur".equals(module.id())) {
            FloatSetting strength = null;
            for (BaseSetting<?> setting : module.settings()) {
                if (setting instanceof FloatSetting fset && "Strength".equalsIgnoreCase(fset.name())) {
                    strength = fset;
                }
            }
            if (strength != null) {
                int totalW = contentW;
                FloatSliderWidget slider = new FloatSliderWidget(contentX, y, totalW, ROW_H, strength);
                this.addDrawableChild(slider);
                scrollItems.add(new ScrollItem(slider, y));
                skipped.add(strength);
                y += ROW_H + ROW_GAP;
            }
        }

        if ("crosshair".equals(module.id())) {
            IntSetting hue = null;
            IntSetting sat = null;
            IntSetting val = null;
            boolean customStyle = ir.modernshadow.bladeclient.config.ConfigManager.get().crosshair.style
                    == ir.modernshadow.bladeclient.config.BladeClientConfig.CrosshairStyle.CUSTOM;
            for (BaseSetting<?> setting : module.settings()) {
                if (setting instanceof IntSetting iset) {
                    if ("Custom Hue".equalsIgnoreCase(iset.name())) hue = iset;
                    else if ("Custom Saturation".equalsIgnoreCase(iset.name())) sat = iset;
                    else if ("Custom Value".equalsIgnoreCase(iset.name())) val = iset;
                }
            }
            if (hue != null && sat != null && val != null) {
                crosshairSwatch = new ColorSwatchRow(contentX, y, contentW, ROW_H, "Color", hue, sat, val, button -> {
                    showCrosshairPicker = !showCrosshairPicker;
                    showOutlinePicker = false;
                    showFillPicker = false;
                    init();
                });
                this.addDrawableChild(crosshairSwatch);
                scrollItems.add(new ScrollItem(crosshairSwatch, y));
                y += ROW_H + ROW_GAP;
                if (showCrosshairPicker) {
                    crosshairPicker = new ColorPickerWidget(contentX, y, contentW, PICKER_H, hue, sat, val);
                    this.addDrawableChild(crosshairPicker);
                    scrollItems.add(new ScrollItem(crosshairPicker, y));
                    y += PICKER_H + ROW_GAP;
                }
                skipped.add(hue);
                skipped.add(sat);
                skipped.add(val);
            }

            if (customStyle) {
                int editorH = 200;
                CrosshairEditorWidget editor = new CrosshairEditorWidget(contentX, y, contentW, editorH,
                        ir.modernshadow.bladeclient.config.ConfigManager.get().crosshair);
                this.addDrawableChild(editor);
                scrollItems.add(new ScrollItem(editor, y));
                y += editorH + ROW_GAP;
            }
        }

        boolean miniFov = "mini_fov".equals(module.id());
        boolean aimingEnabled = false;
        boolean sprintEnabled = false;

        for (BaseSetting<?> setting : module.settings()) {
            if (skipped.contains(setting)) {
                continue;
            }
            if (setting instanceof BooleanSetting bool) {
                int toggleW = ROW_BUTTON_W;
                int toggleX = contentX;
                if (miniFov && "Dynamic Aiming FOV".equalsIgnoreCase(bool.name())) {
                    aimingEnabled = bool.get();
                    ButtonWidget btn = new ToggleRowWidget(toggleX, y, toggleW, ROW_H, bool,
                            () -> miniFovAimingOpen, () -> {
                        miniFovAimingOpen = !miniFovAimingOpen;
                        init();
                    });
                    this.addDrawableChild(btn);
                    rows.add(btn);
                    scrollItems.add(new ScrollItem(btn, y));
                    y += ROW_H + ROW_GAP;
                    continue;
                }
                if (miniFov && "Dynamic Sprinting FOV".equalsIgnoreCase(bool.name())) {
                    sprintEnabled = bool.get();
                    ButtonWidget btn = new ToggleRowWidget(toggleX, y, toggleW, ROW_H, bool,
                            () -> miniFovSprintingOpen, () -> {
                        miniFovSprintingOpen = !miniFovSprintingOpen;
                        init();
                    });
                    this.addDrawableChild(btn);
                    rows.add(btn);
                    scrollItems.add(new ScrollItem(btn, y));
                    y += ROW_H + ROW_GAP;
                    continue;
                }
                ButtonWidget btn = toggleButton(toggleX, y, toggleW, ROW_H, bool);
                this.addDrawableChild(btn);
                rows.add(btn);
                scrollItems.add(new ScrollItem(btn, y));
                y += ROW_H + ROW_GAP;
            } else if (setting instanceof FloatSetting fset) {
                if (miniFov) {
                    String name = fset.name();
                    if ((name.startsWith("Aiming") || name.startsWith("Aiming ")) && (!aimingEnabled || !miniFovAimingOpen)) {
                        continue;
                    }
                    if (name.startsWith("Sprint") && (!sprintEnabled || !miniFovSprintingOpen)) {
                        continue;
                    }
                }
                FloatSliderWidget slider = new FloatSliderWidget(contentX, y, contentW, ROW_H, fset);
                this.addDrawableChild(slider);
                scrollItems.add(new ScrollItem(slider, y));
                y += ROW_H + ROW_GAP;
            } else if (setting instanceof IntSetting iset) {
                IntSliderWidget slider = new IntSliderWidget(contentX, y, contentW, ROW_H, iset);
                this.addDrawableChild(slider);
                scrollItems.add(new ScrollItem(slider, y));
                y += ROW_H + ROW_GAP;
            } else if (setting instanceof EnumSetting<?> eset) {
                int rowW = ROW_BUTTON_W;
                int rowX = contentX;
                ButtonWidget btn = enumButton(rowX, y, rowW, ROW_H, eset);
                this.addDrawableChild(btn);
                rows.add(btn);
                scrollItems.add(new ScrollItem(btn, y));
                y += ROW_H + ROW_GAP;
            } else if (setting instanceof StringSetting sset) {
                int labelW = 110;
                int fieldX = contentX + labelW + 8;
                int fieldW = contentW - labelW - 8;
                net.minecraft.client.gui.widget.TextFieldWidget field =
                        new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, fieldX, y, fieldW, ROW_H, Text.literal(sset.name()));
                field.setText(sset.get() == null ? "" : sset.get());
                field.setMaxLength(sset.maxLength());
                field.setChangedListener(val -> sset.set(val));
                this.addDrawableChild(field);
                stringRows.add(new StringRow(sset.name(), panelX + 20, y, field));
                scrollItems.add(new ScrollItem(field, y));
                y += ROW_H + ROW_GAP;
            } else if (setting instanceof KeybindSetting kset) {
                int rowW = ROW_BUTTON_W;
                int rowX = contentX;
                ButtonWidget btn = keybindButton(rowX, y, rowW, ROW_H, kset);
                this.addDrawableChild(btn);
                rows.add(btn);
                scrollItems.add(new ScrollItem(btn, y));
                y += ROW_H + ROW_GAP;
            }
        }

        if (embeddedMode) {
            ClickableWidget backToMenu = new IconActionButton(panelX + 16, panelY + 12, 20, 20, BACK_ICON, b -> {
                if (onBack != null) {
                    onBack.run();
                }
            });
            this.addDrawableChild(backToMenu);

            ClickableWidget closeAll = new IconActionButton(panelX + panelW - 36, panelY + 12, 20, 20, CLOSE_ICON,
                    b -> MinecraftClient.getInstance().setScreen(null));
            this.addDrawableChild(closeAll);
        } else {
            ButtonWidget back = new GlassButtonWidget(panelX + panelW - 120, panelY + 12, 100, 20,
                    Text.literal("Back"), b -> {
                if (onBack != null) {
                    onBack.run();
                } else {
                    MinecraftClient.getInstance().setScreen(parent);
                }
            });
            this.addDrawableChild(back);
        }

        if (!embeddedMode && module.category() == ModuleCategory.HUD) {
            ButtonWidget hud = new GlassButtonWidget(panelX + 20, panelY + 12, 120, 20,
                    Text.literal("HUD Editor"), b -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(this)));
            this.addDrawableChild(hud);
        }

        updateScrollBounds(panelY, panelH, y);
        applyScroll(panelY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningKeybind != null) {
            InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(button);
            listeningKeybind.set(key);
            if (listeningButton != null) {
                listeningButton.setMessage(Text.literal(label(listeningKeybind.name(),
                        listeningKeybind.keyBinding().getBoundKeyLocalizedText().getString())));
            }
            listeningKeybind = null;
            listeningButton = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (listeningKeybind != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // cancel
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                listeningKeybind.set(InputUtil.UNKNOWN_KEY);
            } else {
                InputUtil.Key bound = InputUtil.fromKeyCode(key, scancode);
                listeningKeybind.set(bound);
            }
            if (listeningButton != null) {
                listeningButton.setMessage(Text.literal(label(listeningKeybind.name(),
                        listeningKeybind.keyBinding().getBoundKeyLocalizedText().getString())));
            }
            listeningKeybind = null;
            listeningButton = null;
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    private ButtonWidget toggleButton(int x, int y, int w, int h, BooleanSetting setting) {
        return new ToggleRowWidget(x, y, w, h, setting);
    }

    private ButtonWidget keybindButton(int x, int y, int w, int h, KeybindSetting setting) {
        return new SettingRowButtonWidget(x, y, w, h, setting.name(),
                () -> (listeningKeybind == setting)
                        ? "Press a key"
                        : setting.keyBinding().getBoundKeyLocalizedText().getString(),
                b -> {
            listeningKeybind = setting;
            listeningButton = b;
        }, true);
    }

    private ButtonWidget enumButton(int x, int y, int w, int h, EnumSetting<?> setting) {
        return new SettingRowButtonWidget(x, y, w, h, setting.name(),
                () -> prettyEnumName(setting.get().name()),
                b -> {
            setting.cycle();
            if ("crosshair".equals(module.id()) && "Style".equalsIgnoreCase(setting.name())) {
                showCrosshairPicker = false;
                init();
            }
        }, true);
    }

    private static String prettyEnumName(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "";
        String[] parts = s.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) out.append(parts[i].substring(1));
        }
        return out.toString();
    }

    private String label(String name, String value) {
        return name + ": " + value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!embeddedMode) {
            context.fill(0, 0, this.width, this.height, 0x80101118);
        }

        int panelX = embeddedMode ? embeddedX : (this.width - PANEL_W) / 2;
        int panelW = embeddedMode ? embeddedW : PANEL_W;
        int panelH = embeddedMode ? embeddedH : getPanelHeight();
        int panelY = embeddedMode ? embeddedY : (this.height - panelH) / 2;

        if (embeddedMode) {
            drawRoundedRect(context, panelX, panelY, panelW, panelH, 10, EMBED_PANEL_BG);
        } else {
            context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x9910131B);
            context.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF3A6DFF);
        }
        BladeFonts.drawUi(context, this.title.getString(), panelX + 112, panelY + 16, 0xFFFFFFFF, BladeFonts.UI_SIZE, true);
        BladeFonts.drawUi(context, "BladeClient > Module: " + module.name(), panelX + 112, panelY + 30, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        if (embeddedMode) {
            BladeFonts.drawUi(context, "Back", panelX + 40, panelY + 17, 0xFFE1E5EC, BladeFonts.UI_SMALL, true);
            BladeFonts.drawUi(context, "Close", panelX + panelW - 72, panelY + 17, 0xFFE1E5EC, BladeFonts.UI_SMALL, true);
        }

        int controlsX = panelX + 14;
        int controlsY = panelY + 42;
        int controlsW = panelW - 28;
        int controlsH = panelH - 54;
        drawRoundedRect(context, controlsX, controlsY, controlsW, controlsH, 8, 0x66101010);
        drawRoundedRect(context, controlsX + 1, controlsY + 1, controlsW - 2, controlsH - 2, 7, 0x551A1D24);

        int contentTop = panelY + (embeddedMode ? 8 : 44);
        int contentBottom = panelY + panelH - 12;
        context.enableScissor(panelX + 10, contentTop, panelX + panelW - 10, contentBottom);

        applyScroll(panelY);
        for (StringRow row : stringRows) {
            BladeFonts.drawUi(context, row.label, row.labelX, row.labelY + 6 - scrollY, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        }

        super.render(context, mouseX, mouseY, delta);
        context.disableScissor();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        int panelX = embeddedMode ? embeddedX : (this.width - PANEL_W) / 2;
        int panelH = embeddedMode ? embeddedH : getPanelHeight();
        int panelY = embeddedMode ? embeddedY : (this.height - panelH) / 2;
        int panelW = embeddedMode ? embeddedW : PANEL_W;
        int contentTop = panelY + (embeddedMode ? 8 : 44);
        int contentBottom = panelY + panelH - 12;
        if (mouseX < panelX + 10 || mouseX > panelX + panelW - 10 || mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollY = (int) Math.max(0, Math.min(maxScroll, scrollY - verticalAmount * 16));
        applyScroll(panelY);
        return true;
    }

    private static final class StringRow {
        final String label;
        final int labelX;
        final int labelY;
        final net.minecraft.client.gui.widget.TextFieldWidget field;

        private StringRow(String label, int labelX, int labelY, net.minecraft.client.gui.widget.TextFieldWidget field) {
            this.label = label;
            this.labelX = labelX;
            this.labelY = labelY;
            this.field = field;
        }
    }

    private int getPanelHeight() {
        int base = 360;
        int extra = 0;
        if ("blockoverlay".equals(module.id())) {
            if (showOutlinePicker) extra += PICKER_H + ROW_GAP;
            if (showFillPicker) extra += PICKER_H + ROW_GAP;
        }
        int target = base + extra;
        int max = Math.max(260, this.height - 40);
        return Math.min(target, max);
    }

    private void updateScrollBounds(int panelY, int panelH, int contentEndY) {
        int contentTop = panelY + 44;
        int contentBottom = panelY + panelH - 12;
        int contentHeight = Math.max(0, contentEndY - contentTop);
        int viewHeight = Math.max(1, contentBottom - contentTop);
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
    }

    private void applyScroll(int panelY) {
        for (ScrollItem item : scrollItems) {
            item.widget.setY(item.baseY - scrollY);
        }
    }

    private static final class ColorSwatchRow extends ButtonWidget {
        private final String label;
        private final IntSetting hue;
        private final IntSetting sat;
        private final IntSetting val;

        private ColorSwatchRow(int x, int y, int w, int h, String label,
                               IntSetting hue, IntSetting sat, IntSetting val, PressAction onPress) {
            super(x, y, w, h, Text.literal(""), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
            this.hue = hue;
            this.sat = sat;
            this.val = val;
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            ctx.fill(x, y, x + w, y + h, 0xFF1F2430);
            ctx.fill(x + 2, y + 1, x + w - 2, y + 2, 0xFF3A6DFF);

            float[] rgb = hsvToRgb(hue.get(), sat.get(), val.get());
            int swatch = toArgb(rgb[0], rgb[1], rgb[2], 1.0f);
            int sw = h - 6;
            int sx = x + 6;
            int sy = y + 3;
            ctx.fill(sx, sy, sx + sw, sy + sw, swatch);
            ctx.fill(sx - 1, sy - 1, sx + sw + 1, sy, 0xFF0B0F18);
            ctx.fill(sx - 1, sy + sw, sx + sw + 1, sy + sw + 1, 0xFF0B0F18);

            String hex = toHex(rgb[0], rgb[1], rgb[2]);
            BladeFonts.drawUi(ctx, label, sx + sw + 8, y + 6, 0xFFFFFFFF, BladeFonts.UI_SMALL, true);
            BladeFonts.drawUi(ctx, hex, x + w - 8 - BladeFonts.uiWidth(hex, BladeFonts.UI_SMALL), y + 6,
                    0xFF9EA7B3, BladeFonts.UI_SMALL, true);
        }

        private static float[] hsvToRgb(int hue, int saturation, int value) {
            float h = ((hue % 360) + 360) % 360;
            float s = clamp01(saturation / 100.0f);
            float v = clamp01(value / 100.0f);
            float c = v * s;
            float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
            float m = v - c;
            float r1, g1, b1;
            if (h < 60) { r1 = c; g1 = x; b1 = 0; }
            else if (h < 120) { r1 = x; g1 = c; b1 = 0; }
            else if (h < 180) { r1 = 0; g1 = c; b1 = x; }
            else if (h < 240) { r1 = 0; g1 = x; b1 = c; }
            else if (h < 300) { r1 = x; g1 = 0; b1 = c; }
            else { r1 = c; g1 = 0; b1 = x; }
            return new float[]{r1 + m, g1 + m, b1 + m};
        }

        private static int toArgb(float r, float g, float b, float a) {
            int ri = Math.round(clamp01(r) * 255.0f);
            int gi = Math.round(clamp01(g) * 255.0f);
            int bi = Math.round(clamp01(b) * 255.0f);
            int ai = Math.round(clamp01(a) * 255.0f);
            return (ai << 24) | (ri << 16) | (gi << 8) | bi;
        }

        private static String toHex(float r, float g, float b) {
            int ri = Math.round(clamp01(r) * 255.0f);
            int gi = Math.round(clamp01(g) * 255.0f);
            int bi = Math.round(clamp01(b) * 255.0f);
            return String.format("#%02X%02X%02X", ri, gi, bi);
        }

        private static float clamp01(float v) {
            if (v < 0.0f) return 0.0f;
            if (v > 1.0f) return 1.0f;
            return v;
        }
    }

    private static final class ScrollItem {
        final net.minecraft.client.gui.widget.ClickableWidget widget;
        final int baseY;

        private ScrollItem(net.minecraft.client.gui.widget.ClickableWidget widget, int baseY) {
            this.widget = widget;
            this.baseY = baseY;
        }
    }

    private static final class IconActionButton extends ClickableWidget {
        private final Identifier icon;
        private final ButtonWidget.PressAction onPress;

        private IconActionButton(int x, int y, int width, int height, Identifier icon, ButtonWidget.PressAction onPress) {
            super(x, y, width, height, Text.literal(""));
            this.icon = icon;
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int bg = isHovered() ? 0x55FFFFFF : 0x33000000;
            drawRoundedRect(context, x, y, w, h, 4, bg);
            float sx = w / 64f;
            float sy = h / 64f;
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.scale(sx, sy);
            int drawX = Math.round(x / sx);
            int drawY = Math.round(y / sy);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, drawX, drawY, 0.0F, 0.0F, 64, 64, 64, 64);
            matrices.popMatrix();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onPress.onPress(null);
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }
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
