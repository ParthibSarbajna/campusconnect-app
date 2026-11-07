package com.campusconnect.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Turns a verified account into an authenticated server-side session (with session-fixation protection). */
@Component
public class SessionLoginHelper {

    private final SecurityContextRepository repository;

    public SessionLoginHelper(SecurityContextRepository repository) {
        this.repository = repository;
    }

    public void establish(AuthUser user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId(); // new session id on privilege change
        }
        Authentication auth = new CurrentAccountAuthenticationToken(
        user, List.of(new SimpleGrantedAuthority(user.role().authority())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}
