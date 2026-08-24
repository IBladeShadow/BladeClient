package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BooleanSetting extends BaseSetting<Boolean> {
    public BooleanSetting(String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter, Runnable onChange) {
        super(name, description, getter, setter, onChange);
    }

    public void toggle() {
        set(!get());
    }
}
