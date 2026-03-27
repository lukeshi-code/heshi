package org.example.model;

public enum LayoutMode {
    SIMPLE("简洁布局"),
    CARD("卡片布局"),
    COMPACT("紧凑布局");

    private final String label;

    LayoutMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
