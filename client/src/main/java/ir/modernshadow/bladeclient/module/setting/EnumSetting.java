package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EnumSetting<E extends Enum<E>> extends BaseSetting<E> {
    private final Class<E> enumClass;

    public EnumSetting(String name, String description, Class<E> enumClass,
                       Supplier<E> getter, Consumer<E> setter, Runnable onChange) {
        super(name, description, getter, setter, onChange);
        this.enumClass = enumClass;
    }

    public Class<E> enumClass() {
        return enumClass;
    }

    public E next() {
        E current = get();
        E[] values = enumClass.getEnumConstants();
        if (values == null || values.length == 0) return current;
        int idx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                idx = i;
                break;
            }
        }
        int next = (idx + 1) % values.length;
        return values[next];
    }

    public E cycle() {
        E next = next();
        set(next);
        return next;
    }
}
