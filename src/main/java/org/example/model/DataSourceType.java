package org.example.model;

public enum DataSourceType {
    MANUAL("手动"),
    AUTO("自动");

    private final String label;

    DataSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
