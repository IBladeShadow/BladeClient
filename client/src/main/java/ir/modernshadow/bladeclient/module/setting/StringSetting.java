package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StringSetting extends BaseSetting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, int maxLength,
                         Supplier<String> getter, Consumer<String> setter, Runnable onChange) {
        super(name, description, getter, setter, onChange);
        this.maxLength = Math.max(1, maxLength);
    }

    public int maxLength() {
        return maxLength;
    }
}
