package ir.modernshadow.bladeclient.screen;

import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.widget.GlassButtonWidget;
import ir.modernshadow.bladeclient.skin.SkinManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkinManagerScreen extends Screen {
    private static final int PANEL_MARGIN = 24;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 8;
    private static final int PREVIEW_W = 140;
    private static final int PREVIEW_H = 180;

    private final Screen parent;
    private TextFieldWidget skinField;
    private TextFieldWidget nameField;
    private ButtonWidget modeBtn;
    private ButtonWidget modelBtn;
    private ButtonWidget enableBtn;
    private ButtonWidget showMojangBtn;
    private ButtonWidget showVanillaCapeBtn;
    private ButtonWidget showOptifineCapeBtn;
    private ButtonWidget applyBtn;
    private PlayerSkinWidget preview;
    private int previewX;
    private int previewY;
    private final List<ButtonWidget> presetButtons = new ArrayList<>();
    private boolean browsing = false;

    public SkinManagerScreen(Screen parent) {
        super(Text.literal("Skin Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        presetButtons.clear();

        int panelX = PANEL_MARGIN;
        int panelY = PANEL_MARGIN;
        int panelW = this.width - PANEL_MARGIN * 2;
        int panelH = this.height - PANEL_MARGIN * 2;

        int leftX = panelX + 20;
        int leftW = Math.min(320, panelW - 220);
        int y = panelY + 48;

        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;

        enableBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("Enabled: " + (cfg.enabled ? "ON" : "OFF")),
                b -> {
                    cfg.enabled = !cfg.enabled;
                    b.setMessage(Text.literal("Enabled: " + (cfg.enabled ? "ON" : "OFF")));
                    ConfigManager.saveQuiet();
                });
        addDrawableChild(enableBtn);
        y += ROW_H + ROW_GAP;

        modeBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("Mode: " + cfg.mode.name()),
                b -> {
                    cfg.mode = (cfg.mode == BladeClientConfig.SkinMode.FILE)
                            ? BladeClientConfig.SkinMode.USERNAME
                            : BladeClientConfig.SkinMode.FILE;
                    b.setMessage(Text.literal("Mode: " + cfg.mode.name()));
                    if (skinField != null) {
                        skinField.setText(cfg.mode == BladeClientConfig.SkinMode.FILE
                                ? (cfg.filePath == null ? "" : cfg.filePath)
                                : (cfg.username == null ? "" : cfg.username));
                    }
                    ConfigManager.saveQuiet();
                });
        addDrawableChild(modeBtn);
        y += ROW_H + ROW_GAP;

        modelBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("Model: " + cfg.model.name()),
                b -> {
                    cfg.model = switch (cfg.model) {
                        case AUTO -> BladeClientConfig.SkinModel.WIDE;
                        case WIDE -> BladeClientConfig.SkinModel.SLIM;
                        case SLIM -> BladeClientConfig.SkinModel.AUTO;
                    };
                    b.setMessage(Text.literal("Model: " + cfg.model.name()));
                    ConfigManager.saveQuiet();
                });
        addDrawableChild(modelBtn);
        y += ROW_H + ROW_GAP;

        boolean showMojang = cfg.showMojangSkins == null || cfg.showMojangSkins;
        showMojangBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("Mojang Skins: " + (showMojang ? "ON" : "OFF")),
                b -> {
                    boolean next = !(cfg.showMojangSkins == null || cfg.showMojangSkins);
                    cfg.showMojangSkins = next;
                    b.setMessage(Text.literal("Mojang Skins: " + (next ? "ON" : "OFF")));
                    ConfigManager.saveQuiet();
                    ir.modernshadow.bladeclient.skin.PremiumSkinManager.requestRetry();
                    SkinManager.requestPreview(MinecraftClient.getInstance());
                });
        addDrawableChild(showMojangBtn);
        y += ROW_H + ROW_GAP;

        boolean showVanilla = cfg.showVanillaCape == null || cfg.showVanillaCape;
        showVanillaCapeBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("Vanilla Cape: " + (showVanilla ? "ON" : "OFF")),
                b -> {
                    boolean next = !(cfg.showVanillaCape == null || cfg.showVanillaCape);
                    cfg.showVanillaCape = next;
                    b.setMessage(Text.literal("Vanilla Cape: " + (next ? "ON" : "OFF")));
                    ConfigManager.saveQuiet();
                    SkinManager.requestPreview(MinecraftClient.getInstance());
                });
        addDrawableChild(showVanillaCapeBtn);
        y += ROW_H + ROW_GAP;

        boolean showOptifine = cfg.showOptifineCape == null || cfg.showOptifineCape;
        showOptifineCapeBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H,
                Text.literal("OptiFine Cape: " + (showOptifine ? "ON" : "OFF")),
                b -> {
                    boolean next = !(cfg.showOptifineCape == null || cfg.showOptifineCape);
                    cfg.showOptifineCape = next;
                    b.setMessage(Text.literal("OptiFine Cape: " + (next ? "ON" : "OFF")));
                    ConfigManager.saveQuiet();
                    ir.modernshadow.bladeclient.skin.OptifineCapeManager.clearCache();
                    SkinManager.requestPreview(MinecraftClient.getInstance());
                });
        addDrawableChild(showOptifineCapeBtn);
        y += ROW_H + ROW_GAP;

        skinField = new TextFieldWidget(this.textRenderer, leftX, y, leftW, ROW_H,
                Text.literal(cfg.mode == BladeClientConfig.SkinMode.FILE ? "File" : "Username"));
        skinField.setMaxLength(260);
        skinField.setText(cfg.mode == BladeClientConfig.SkinMode.FILE
                ? (cfg.filePath == null ? "" : cfg.filePath)
                : (cfg.username == null ? "" : cfg.username));
        skinField.setChangedListener(val -> {
            if (cfg.mode == BladeClientConfig.SkinMode.FILE) {
                cfg.filePath = val;
            } else {
                cfg.username = val;
            }
            ConfigManager.saveQuiet();
        });
        addDrawableChild(skinField);
        y += ROW_H + ROW_GAP;

        nameField = new TextFieldWidget(this.textRenderer, leftX, y, leftW, ROW_H, Text.literal("Preset name"));
        nameField.setText("");
        nameField.setMaxLength(48);
        addDrawableChild(nameField);
        y += ROW_H + ROW_GAP;

        applyBtn = new GlassButtonWidget(leftX, y, leftW, ROW_H, Text.literal("Apply"),
                b -> {
                    SkinManager.applyNow(MinecraftClient.getInstance());
                    savePreset(cfg);
                    refreshPresets(panelX, panelY, panelW, panelH);
                });
        addDrawableChild(applyBtn);
        y += ROW_H + ROW_GAP;

        refreshPresets(panelX, panelY, panelW, panelH);

        previewX = panelX + panelW - PREVIEW_W - 20;
        previewY = panelY + 60;
        MinecraftClient mc = MinecraftClient.getInstance();
        GameProfile profile = buildProfile(mc);
        preview = new PlayerSkinWidget(PREVIEW_W, PREVIEW_H, mc.getLoadedEntityModels(),
                () -> {
                    SkinTextures base = mc.getSkinProvider().getSkinTextures(profile);
                    SkinTextures resolved = ir.modernshadow.bladeclient.skin.SkinResolver.resolve(profile, base);
                    return SkinManager.getPreviewOrOverride(profile, resolved != null ? resolved : base);
                });
        preview.setX(previewX);
        preview.setY(previewY);
        addDrawableChild(preview);

        ButtonWidget back = new GlassButtonWidget(panelX + panelW - 110, panelY + 12, 90, 20,
                Text.literal("Back"), b -> MinecraftClient.getInstance().setScreen(parent));
        addDrawableChild(back);
    }

    @Override
    public void tick() {
        super.tick();
        SkinManager.requestPreview(MinecraftClient.getInstance());
    }

    private void refreshPresets(int panelX, int panelY, int panelW, int panelH) {
        for (ButtonWidget btn : presetButtons) {
            remove(btn);
        }
        presetButtons.clear();

        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        int listX = panelX + 20;
        int listW = Math.min(320, panelW - 220);
        int rowsBeforePresets = 9;
        int y = panelY + 48 + (ROW_H + ROW_GAP) * rowsBeforePresets;
        int maxY = panelY + panelH - 30;

        for (int i = 0; i < cfg.presets.size(); i++) {
            if (y + ROW_H > maxY) break;
            BladeClientConfig.SkinPreset preset = cfg.presets.get(i);
            String label = preset.name == null || preset.name.isBlank() ? "(Unnamed)" : preset.name;
            ButtonWidget use = new GlassButtonWidget(listX, y, listW - 54, ROW_H, Text.literal(label), b -> {
                loadPreset(preset);
                SkinManager.requestPreview(MinecraftClient.getInstance());
            });
            ButtonWidget del = new GlassButtonWidget(listX + listW - 46, y, 46, ROW_H, Text.literal("Del"), b -> {
                cfg.presets.remove(preset);
                ConfigManager.saveQuiet();
                refreshPresets(panelX, panelY, panelW, panelH);
            });
            addDrawableChild(use);
            addDrawableChild(del);
            presetButtons.add(use);
            presetButtons.add(del);
            y += ROW_H + ROW_GAP;
        }
    }

    private void loadPreset(BladeClientConfig.SkinPreset preset) {
        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        cfg.mode = preset.mode;
        cfg.model = preset.model;
        cfg.filePath = preset.filePath == null ? "" : preset.filePath;
        cfg.username = preset.username == null ? "" : preset.username;
        ConfigManager.saveQuiet();

        modeBtn.setMessage(Text.literal("Mode: " + cfg.mode.name()));
        modelBtn.setMessage(Text.literal("Model: " + cfg.model.name()));
        skinField.setText(cfg.mode == BladeClientConfig.SkinMode.FILE ? cfg.filePath : cfg.username);
    }

    private void savePreset(BladeClientConfig.SkinChanger cfg) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = autoName(cfg);
        }
        if (name.isEmpty()) return;

        String key = presetKey(cfg);
        for (BladeClientConfig.SkinPreset existing : cfg.presets) {
            if (presetKey(existing).equals(key)) {
                existing.name = name;
                ConfigManager.saveQuiet();
                return;
            }
        }
        BladeClientConfig.SkinPreset preset = new BladeClientConfig.SkinPreset();
        preset.name = name;
        preset.mode = cfg.mode;
        preset.model = cfg.model;
        preset.filePath = cfg.filePath == null ? "" : cfg.filePath;
        preset.username = cfg.username == null ? "" : cfg.username;
        cfg.presets.add(preset);
        ConfigManager.saveQuiet();
    }

    private String autoName(BladeClientConfig.SkinChanger cfg) {
        if (cfg.mode == BladeClientConfig.SkinMode.FILE) {
            if (cfg.filePath != null && !cfg.filePath.isBlank()) {
                Path p = Paths.get(cfg.filePath);
                return p.getFileName().toString();
            }
        } else {
            if (cfg.username != null && !cfg.username.isBlank()) {
                return cfg.username.trim();
            }
        }
        return "";
    }

    private String presetKey(BladeClientConfig.SkinChanger cfg) {
        return cfg.mode + "|" + cfg.model + "|" + cfg.filePath + "|" + cfg.username;
    }

    private String presetKey(BladeClientConfig.SkinPreset preset) {
        return preset.mode + "|" + preset.model + "|" + preset.filePath + "|" + preset.username;
    }

    private GameProfile buildProfile(MinecraftClient mc) {
        if (mc == null) return new GameProfile(new UUID(0, 0), "Player");
        var session = mc.getSession();
        UUID id = session.getUuidOrNull() != null ? session.getUuidOrNull() : new UUID(0, 0);
        String name = session.getUsername() != null ? session.getUsername() : "Player";
        return new GameProfile(id, name);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80101118);

        int panelX = PANEL_MARGIN;
        int panelY = PANEL_MARGIN;
        int panelW = this.width - PANEL_MARGIN * 2;
        int panelH = this.height - PANEL_MARGIN * 2;

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x9910131B);
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF3A6DFF);
        BladeFonts.drawUi(context, this.title.getString(), panelX + 16, panelY + 16, 0xFFFFFFFF, BladeFonts.UI_SIZE, true);

        BladeFonts.drawUi(context, "Preview", previewX, panelY + 38, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        BladeFonts.drawUi(context, "Saved Skins", panelX + 20, panelY + 48 + (ROW_H + ROW_GAP) * 8, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);

        drawPreviewFrame(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && skinField != null) {
            BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
            if (cfg.mode == BladeClientConfig.SkinMode.FILE) {
                int x = skinField.getX();
                int y = skinField.getY();
                int w = skinField.getWidth();
                int h = skinField.getHeight();
                if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    openFileChooser();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openFileChooser() {
        if (browsing) return;
        browsing = true;
        new Thread(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Skin PNG");
                chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (file != null) {
                        MinecraftClient.getInstance().execute(() -> {
                            BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
                            cfg.filePath = file.getAbsolutePath();
                            skinField.setText(cfg.filePath);
                            ConfigManager.saveQuiet();
                        });
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                browsing = false;
            }
        }, "BladeClient-FileChooser").start();
    }

    private void drawPreviewFrame(DrawContext context) {
        if (preview == null) return;
        int x = preview.getX() - 2;
        int y = preview.getY() - 2;
        int w = preview.getWidth() + 4;
        int h = preview.getHeight() + 4;

        int outer = 0xFF0B0F18;
        int inner = 0xFF161C27;
        int highlight = 0xFF2A3344;
        int shadow = 0xFF06080D;

        context.fill(x, y, x + w, y + h, outer);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, inner);
        context.fill(x + 1, y + 1, x + w - 1, y + 2, highlight);
        context.fill(x + 1, y + 1, x + 2, y + h - 1, highlight);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, shadow);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, shadow);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
