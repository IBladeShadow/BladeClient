package ir.modernshadow.bladeclient.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import ir.modernshadow.bladeclient.screen.widget.GlassOwoButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BladePauseScreen extends BaseOwoScreen<FlowLayout> {
    private static final int ICON_SIZE = 56;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final float BUTTON_TEXT_SCALE = 0.9f;

    public BladePauseScreen() {
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
        root.padding(Insets.of(8));
        root.gap(12);

        FlowLayout header = Containers.verticalFlow(Sizing.content(), Sizing.content());
        header.horizontalAlignment(HorizontalAlignment.CENTER);
        header.gap(4);

        Identifier iconId = BladeLogoMask.iconId(this.client);
        TextureComponent icon = Components.texture(iconId, 0, 0, 512, 512, 512, 512);
        icon.sizing(Sizing.fixed(ICON_SIZE), Sizing.fixed(ICON_SIZE));
        icon.blend(true);

        LabelComponent title = Components.label(Text.literal("BladeClient"))
                .color(Color.ofArgb(0xFF4AA3FF))
                .shadow(true);

        header.child(icon);
        header.child(title);

        FlowLayout buttons = Containers.verticalFlow(Sizing.content(), Sizing.content());
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);
        buttons.gap(BUTTON_GAP);

        buttons.child(makeButton(Text.translatable("menu.returnToGame"), () -> this.client.setScreen(null)));
        buttons.child(makeHalfRow());
        buttons.child(makeButton(Text.translatable("menu.options"), () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));

        Text quitText = this.client != null && this.client.isInSingleplayer()
                ? Text.translatable("menu.returnToMenu")
                : Text.translatable("menu.disconnect");

        buttons.child(makeButton(quitText, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            GameMenuScreen.disconnect(client, Text.translatable("menu.savingLevel"));
        }));

        root.child(header);
        root.child(buttons);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private static ButtonComponent makeButton(Text label, Runnable action) {
        GlassOwoButton button = new GlassOwoButton(label, b -> action.run(), BUTTON_TEXT_SCALE, true);
        button.sizing(Sizing.fixed(BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
        return button;
    }

    private FlowLayout makeHalfRow() {
        int halfWidth = (BUTTON_WIDTH - BUTTON_GAP) / 2;

        FlowLayout row = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        row.horizontalAlignment(HorizontalAlignment.CENTER);
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(BUTTON_GAP);

        GlassOwoButton bladeButton = new GlassOwoButton(Text.literal("BladeClient"),
                b -> this.client.setScreen(new BladeClientMenuScreen(this)), BUTTON_TEXT_SCALE, true);
        bladeButton.sizing(Sizing.fixed(halfWidth), Sizing.fixed(BUTTON_HEIGHT));

        GlassOwoButton multiButton = new GlassOwoButton(Text.translatable("menu.multiplayer"),
                b -> this.client.setScreen(new MultiplayerScreen(this)), BUTTON_TEXT_SCALE, true);
        multiButton.sizing(Sizing.fixed(halfWidth), Sizing.fixed(BUTTON_HEIGHT));

        row.child(bladeButton);
        row.child(multiButton);
        return row;
    }
}
