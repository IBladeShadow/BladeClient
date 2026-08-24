package ir.modernshadow.bladeclient.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseSetting<T> {
    private final String name;
    private final String description;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Runnable onChange;

    protected BaseSetting(String name, String description, Supplier<T> getter, Consumer<T> setter, Runnable onChange) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.onChange = onChange;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public T get() {
        return getter.get();
    }

    public void set(T value) {
        setter.accept(value);
        if (onChange != null) onChange.run();
    }
}
