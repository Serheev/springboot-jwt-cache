package com.serheev.jwtAuth.model;

import org.springframework.security.core.GrantedAuthority;

public enum RoleName implements GrantedAuthority {
	USER,
    DEVELOPER,
    ADMIN;

    @Override
    public String getAuthority() {
            return "ROLE_" + name();
    }
}
