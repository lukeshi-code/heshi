package org.example.model;

public enum AccessLevel {
    PUBLIC("公开"),
    USER("成员及以上"),
    EDITOR("编辑及以上"),
    ADMIN("仅管理员");

    private final String label;

    AccessLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
