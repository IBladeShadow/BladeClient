package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.widget.GlassButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PlayerQuickActionScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 120;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;

    private final String targetName;

    public PlayerQuickActionScreen(String targetName) {
        super(Text.literal("Player Actions"));
        this.targetName = targetName;
    }

    @Override
    protected void init() {
        clearChildren();
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        int btnX = x + 12;
        int btnW = PANEL_W - 24;
        int by = y + 34;

        addDrawableChild(new GlassButtonWidget(btnX, by, btnW, ROW_H, Text.literal("Friend"),
                b -> sendFriend()));
        by += ROW_H + ROW_GAP;

        addDrawableChild(new GlassButtonWidget(btnX, by, btnW, ROW_H, Text.literal("Party"),
                b -> sendPartyInvite()));
        by += ROW_H + ROW_GAP;

        addDrawableChild(new GlassButtonWidget(btnX, by, btnW, ROW_H, Text.literal("Duel"),
                b -> sendCommand("/duel " + targetName)));
    }

    private void sendFriend() {
        sendCommand("/friend add " + targetName);
    }

    private void sendPartyInvite() {
        sendCommand(resolvePartyInvite() + " " + targetName);
    }

    private String resolvePartyInvite() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (hasCommand(client, "party")) {
            return "/party invite";
        }
        if (hasCommand(client, "p")) {
            return "/p invite";
        }
        return "/party invite";
    }

    private boolean hasCommand(MinecraftClient client, String name) {
        if (client == null || client.getNetworkHandler() == null) {
            return false;
        }
        var dispatcher = client.getNetworkHandler().getCommandDispatcher();
        if (dispatcher == null) {
            return false;
        }
        return dispatcher.getRoot().getChild(name) != null;
    }

    private void sendCommand(String cmd) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            client.setScreen(null);
            return;
        }
        if (cmd.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(cmd.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(cmd);
        }
        client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x66000000);
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        context.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF10131B);
        context.fill(x, y, x + PANEL_W, y + 2, 0xFF3A6DFF);
        BladeFonts.drawUiCentered(context, targetName, x + PANEL_W / 2.0f, y + 16,
                0xFFFFFFFF, BladeFonts.UI_SIZE, true);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
