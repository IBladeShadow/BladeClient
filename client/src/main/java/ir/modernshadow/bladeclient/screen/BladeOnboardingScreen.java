package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.account.AccountManager;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BladeOnboardingScreen extends Screen {
    private TextFieldWidget nameField;
    private boolean applied = false;

    public BladeOnboardingScreen() {
        super(Text.literal("Welcome"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        nameField = new TextFieldWidget(this.textRenderer, cx - 80, cy + 10, 160, 20, Text.literal("Username"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Enter your username"));
        addDrawableChild(nameField);
        setInitialFocus(nameField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF0D0D1A);

        int cx = this.width / 2;
        int cy = this.height / 2;

        String title = "Welcome to BladeClient";
        int tw = BladeFonts.titleWidth(title, 2.5f);
        BladeFonts.drawTitle(context, title, (cx - tw / 2f), cy - 70, 0xFF4AA3FF, 2.5f, true);

        String msg = "Please enter your username to continue";
        int mw = BladeFonts.uiWidth(msg, BladeFonts.UI_SIZE);
        BladeFonts.drawUi(context, msg, (cx - mw / 2f), cy - 25, 0xFFB6BDC8, BladeFonts.UI_SIZE, true);

        nameField.render(context, mouseX, mouseY, delta);

        String hint = "Username must be 3-16 characters (a-z, 0-9, _)";
        int hw = BladeFonts.uiWidth(hint, BladeFonts.UI_SMALL);
        BladeFonts.drawUi(context, hint, (cx - hw / 2f), cy + 40, 0xFF8B929E, BladeFonts.UI_SMALL, true);

        String name = nameField.getText().trim();
        if (!name.isEmpty() && name.length() >= 3) {
            String enter = "Press Enter to continue";
            int ew = BladeFonts.uiWidth(enter, BladeFonts.UI_SMALL);
            BladeFonts.drawUi(context, enter, (cx - ew / 2f), cy + 60, 0xFF4CAF50, BladeFonts.UI_SMALL, true);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && !applied) {
            String name = nameField.getText().trim();
            if (name.length() >= 3 && name.length() <= 16 && name.matches("[A-Za-z0-9_]+")) {
                applied = true;
                var cfg = ConfigManager.get().account;
                cfg.useOffline = true;
                cfg.offlineName = name;
                cfg.offlineAccounts.clear();
                cfg.offlineAccounts.add(name);
                ConfigManager.saveQuiet();
                AccountManager.applyOffline(MinecraftClient.getInstance(), name);
                MinecraftClient.getInstance().setScreen(null);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
