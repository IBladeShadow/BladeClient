package ir.modernshadow.bladeclient.module;

import ir.modernshadow.bladeclient.module.setting.BaseSetting;
import ir.modernshadow.bladeclient.module.setting.BooleanSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Module {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final BooleanSetting enabledSetting;
    private final List<BaseSetting<?>> settings;

    public Module(String id,
                  String name,
                  String description,
                  ModuleCategory category,
                  BooleanSetting enabledSetting,
                  List<BaseSetting<?>> settings) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledSetting = enabledSetting;
        this.settings = settings == null ? new ArrayList<>() : new ArrayList<>(settings);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public ModuleCategory category() {
        return category;
    }

    public BooleanSetting enabledSetting() {
        return enabledSetting;
    }

    public boolean isEnabled() {
        return enabledSetting != null && Boolean.TRUE.equals(enabledSetting.get());
    }

    public void toggle() {
        if (enabledSetting != null) enabledSetting.toggle();
    }

    public List<BaseSetting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }
}
