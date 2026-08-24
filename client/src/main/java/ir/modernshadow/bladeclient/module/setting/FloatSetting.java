package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FloatSetting extends BaseSetting<Float> {
    private final float min;
    private final float max;
    private final float step;

    public FloatSetting(String name, String description, float min, float max, float step,
                        Supplier<Float> getter, Consumer<Float> setter, Runnable onChange) {
        super(name, description, getter, setter, onChange);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public float step() {
        return step;
    }
}
