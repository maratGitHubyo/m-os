package com.mos.security;

import java.security.Principal;
import java.util.UUID;

public record StompUserPrincipal(MosUserPrincipal principal) implements Principal {

    @Override
    public String getName() {
        return principal.userId().toString();
    }

    public UUID userId() {
        return principal.userId();
    }

    public UUID gameSessionId() {
        return principal.gameSessionId();
    }
}
