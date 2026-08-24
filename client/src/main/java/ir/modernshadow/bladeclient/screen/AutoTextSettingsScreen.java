package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.widget.GlassButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AutoTextSettingsScreen extends Screen {
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 300;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private ButtonWidget addBtn;
    private ButtonWidget enabledBtn;
    private BladeClientConfig.AutoTextEntry listeningEntry;
    private ButtonWidget listeningButton;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listBottom;

    public AutoTextSettingsScreen(Screen parent) {
        super(Text.literal("Auto Text"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        rows.clear();

        panelW = Math.min(PANEL_W, this.width - 32);
        panelH = Math.min(PANEL_H, this.height - 32);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int x = panelX + 16;
        int w = panelW - 32;

        int backY = panelY + panelH - ROW_H - 12;
        int addY = backY - ROW_H - 8;

        enabledBtn = new GlassButtonWidget(x, panelY + 16, 140, ROW_H,
                Text.literal(enabledLabel()),
                b -> toggleEnabled());
        addDrawableChild(enabledBtn);

        addBtn = new GlassButtonWidget(x + w - 40, addY, 40, ROW_H,
                Text.literal("+"),
                b -> addEntry());
        addDrawableChild(addBtn);

        ButtonWidget back = new GlassButtonWidget(x, backY, w - 48, ROW_H, Text.literal("Back"),
                b -> MinecraftClient.getInstance().setScreen(parent));
        addDrawableChild(back);

        listX = x;
        listY = panelY + 48;
        listW = w;
        listBottom = addY - 8;

        refreshRows();
    }

    private void refreshRows() {
        for (Row row : rows) {
            remove(row.messageField);
            remove(row.keyButton);
            remove(row.removeButton);
        }
        rows.clear();

        BladeClientConfig.AutoText cfg = ConfigManager.get().autoText;
        if (cfg.entries == null || cfg.entries.isEmpty()) {
            cfg.entries = new ArrayList<>();
            cfg.entries.add(new BladeClientConfig.AutoTextEntry());
            ConfigManager.saveQuiet();
        }

        int keyW = 78;
        int delW = 24;
        int gap = 6;
        int msgW = Math.max(120, listW - keyW - delW - gap * 2);

        int y = listY;
        for (int i = 0; i < cfg.entries.size(); i++) {
            BladeClientConfig.AutoTextEntry entry = cfg.entries.get(i);
            if (entry == null) continue;
            if (y + ROW_H > listBottom) break;

            TextFieldWidget messageField = new TextFieldWidget(this.textRenderer, listX, y, msgW, ROW_H, Text.literal("Message"));
            messageField.setMaxLength(256);
            messageField.setText(entry.message == null ? "" : entry.message);
            messageField.setChangedListener(val -> {
                entry.message = val;
                ConfigManager.saveQuiet();
            });
            addDrawableChild(messageField);

            ButtonWidget keyButton = new GlassButtonWidget(listX + msgW + gap, y, keyW, ROW_H,
                    Text.literal(keyLabel(entry)),
                    b -> beginKeybind(entry, b));
            addDrawableChild(keyButton);

            ButtonWidget removeButton = new GlassButtonWidget(listX + msgW + gap + keyW + gap, y, delW, ROW_H,
                    Text.literal("X"),
                    b -> removeEntry(entry));
            addDrawableChild(removeButton);

            rows.add(new Row(entry, messageField, keyButton, removeButton));
            y += ROW_H + ROW_GAP;
        }
    }

    private void toggleEnabled() {
        BladeClientConfig.AutoText cfg = ConfigManager.get().autoText;
        cfg.enabled = !cfg.enabled;
        ConfigManager.saveQuiet();
        if (enabledBtn != null) enabledBtn.setMessage(Text.literal(enabledLabel()));
    }

    private String enabledLabel() {
        return "Enabled: " + (ConfigManager.get().autoText.enabled ? "ON" : "OFF");
    }

    private void addEntry() {
        BladeClientConfig.AutoText cfg = ConfigManager.get().autoText;
        cfg.entries.add(new BladeClientConfig.AutoTextEntry());
        ConfigManager.saveQuiet();
        refreshRows();
    }

    private void removeEntry(BladeClientConfig.AutoTextEntry entry) {
        BladeClientConfig.AutoText cfg = ConfigManager.get().autoText;
        if (cfg.entries.size() <= 1) {
            entry.message = "";
            entry.intervalSeconds = 10;
            entry.keyCode = -1;
            entry.keyType = 0;
            ConfigManager.saveQuiet();
            refreshRows();
            return;
        }
        cfg.entries.remove(entry);
        ConfigManager.saveQuiet();
        refreshRows();
    }

    private void beginKeybind(BladeClientConfig.AutoTextEntry entry, ButtonWidget btn) {
        listeningEntry = entry;
        listeningButton = btn;
        btn.setMessage(Text.literal("Press"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningEntry != null) {
            InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(button);
            applyKey(listeningEntry, key);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (listeningEntry != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // cancel
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                applyKey(listeningEntry, InputUtil.UNKNOWN_KEY);
            } else {
                applyKey(listeningEntry, InputUtil.Type.KEYSYM.createFromCode(key));
            }
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    private void applyKey(BladeClientConfig.AutoTextEntry entry, InputUtil.Key key) {
        if (entry == null) return;
        if (key == null || key == InputUtil.UNKNOWN_KEY) {
            entry.keyCode = -1;
            entry.keyType = 0;
        } else {
            entry.keyType = key.getCategory().ordinal();
            entry.keyCode = key.getCode();
        }
        ConfigManager.saveQuiet();
        if (listeningButton != null) {
            listeningButton.setMessage(Text.literal(keyLabel(entry)));
        }
        listeningEntry = null;
        listeningButton = null;
    }

    private String keyLabel(BladeClientConfig.AutoTextEntry entry) {
        if (entry == null || entry.keyCode < 0) return "Key";
        InputUtil.Type type = entry.keyType == InputUtil.Type.MOUSE.ordinal() ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
        InputUtil.Key key = type.createFromCode(entry.keyCode);
        return key.getLocalizedText().getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiTheme.drawBackground(context, this.width, this.height);
        UiTheme.drawPanel(context, panelX, panelY, panelW, panelH);

        BladeFonts.drawUi(context, "Auto Text", panelX + 16, panelY + 6, UiTheme.TEXT_PRIMARY, BladeFonts.UI_SIZE, true);
        BladeFonts.drawUi(context, "Message list", panelX + 16, panelY + 28, UiTheme.TEXT_MUTED, BladeFonts.UI_SMALL, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Row(BladeClientConfig.AutoTextEntry entry,
                       TextFieldWidget messageField,
                       ButtonWidget keyButton,
                       ButtonWidget removeButton) {}
}
