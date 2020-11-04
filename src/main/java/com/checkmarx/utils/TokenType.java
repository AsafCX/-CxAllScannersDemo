package com.checkmarx.utils;

public enum TokenType {
    REFRESH ("refresh-token"),
    ACCESS ("access-token");

    private final String type;

    TokenType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}

