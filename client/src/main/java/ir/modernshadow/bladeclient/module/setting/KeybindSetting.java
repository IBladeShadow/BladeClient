package ir.modernshadow.bladeclient.module.setting;

import ir.modernshadow.bladeclient.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class KeybindSetting extends BaseSetting<InputUtil.Key> {
    private final KeyBinding keyBinding;

    public KeybindSetting(String name, String description, KeyBinding keyBinding) {
        super(name, description,
                () -> ((KeyBindingAccessor) keyBinding).bladeclient$getBoundKey(),
                key -> {
                    keyBinding.setBoundKey(key);
                    KeyBinding.updateKeysByCode();
                },
                () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.options != null) {
                        mc.options.write();
                    }
                });
        this.keyBinding = keyBinding;
    }

    public KeyBinding keyBinding() {
        return keyBinding;
    }
}
