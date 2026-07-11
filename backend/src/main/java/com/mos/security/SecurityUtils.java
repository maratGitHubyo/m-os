package com.mos.security;

import com.mos.common.exception.InvalidCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static MosUserPrincipal getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getPrincipal();
        }

        throw new InvalidCredentialsException("Authentication required");
    }
}
