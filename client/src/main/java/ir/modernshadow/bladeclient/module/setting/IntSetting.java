package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class IntSetting extends BaseSetting<Integer> {
    private final int min;
    private final int max;
    private final int step;

    public IntSetting(String name, String description, int min, int max, int step,
                      Supplier<Integer> getter, Consumer<Integer> setter, Runnable onChange) {
        super(name, description, getter, setter, onChange);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int step() {
        return step;
    }
}
