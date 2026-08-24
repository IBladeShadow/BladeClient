package ir.modernshadow.bladeclient.module;

public enum ModuleCategory {
    ALL("All"),
    HUD("HUD"),
    VISUAL("Visual"),
    UI("UI"),
    UTILITY("Utility"),
    SOON("Soon...");

    public final String label;

    ModuleCategory(String label) {
        this.label = label;
    }
}
