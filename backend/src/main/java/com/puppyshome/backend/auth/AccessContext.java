package com.puppyshome.backend.auth;

import java.util.Set;

public record AccessContext(Long userId, Set<String> roles) {

    public boolean admin() {
        return roles.contains(RoleEntity.CODE_ADMIN);
    }

    public boolean customer() {
        return roles.contains(RoleEntity.CODE_CUSTOMER);
    }

    public boolean employee() {
        return roles.contains(RoleEntity.CODE_EMPLOYEE);
    }
}
