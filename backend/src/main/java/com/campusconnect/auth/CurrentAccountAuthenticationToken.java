package com.campusconnect.auth;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class CurrentAccountAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public CurrentAccountAuthenticationToken(
            Object principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, null, authorities);
    }
}