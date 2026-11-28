package com.puppyshome.backend.auth;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccessContextResolver {

    private final UserRepository userRepository;

    public AccessContextResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public AccessContext resolve(Authentication authentication) {
        UserEntity user = userRepository
                .findByLoginIdentifier(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng."));
        Set<String> roles = user.getRoles()
                .stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toUnmodifiableSet());
        return new AccessContext(user.getId(), roles);
    }
}
