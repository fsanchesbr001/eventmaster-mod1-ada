package com.fabriciosanches.userservice.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
    SYSTEM("ROLE_SYSTEM");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
