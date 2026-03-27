package org.example.model;

public enum LinkType {
    INTERNAL("内部页面"),
    EXTERNAL("外部链接");

    private final String label;

    LinkType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
