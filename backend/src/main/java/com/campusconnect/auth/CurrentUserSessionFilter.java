package com.campusconnect.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refreshes genuine application-session authentication using the current
 * persisted account.
 *
 * Role and department changes take effect on subsequent requests.
 * Deleted or disabled accounts cannot continue using an existing session.
 */
public class CurrentUserSessionFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final SecurityContextRepository contextRepository;

    public CurrentUserSessionFilter(
            UserRepository users,
            SecurityContextRepository contextRepository) {
        this.users = users;
        this.contextRepository = contextRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication current =
                SecurityContextHolder.getContext().getAuthentication();

        // Refresh only authentication established through the real login flow.
        // Test-created authentication and other principals remain untouched.
        if (current == null
                || !current.isAuthenticated()
                || !(current instanceof CurrentAccountAuthenticationToken)
                || !(current.getPrincipal() instanceof AuthUser principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UserAccount> account =
                users.findById(principal.id())
                        .filter(UserAccount::isEnabled);

        // The account was deleted or disabled after login.
        if (account.isEmpty()) {
            SecurityContextHolder.clearContext();

            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"detail\":\"Not signed in.\"}");
            return;
        }

        AuthUser refreshed = AuthUser.from(account.get());

        // Persist changes only when the account details have changed.
        if (!refreshed.equals(principal)) {
            CurrentAccountAuthenticationToken updated =
                    new CurrentAccountAuthenticationToken(
                            refreshed,
                            List.of(new SimpleGrantedAuthority(
                                    refreshed.role().authority())));

            updated.setDetails(current.getDetails());

            SecurityContext context =
                    SecurityContextHolder.getContext();
            context.setAuthentication(updated);

            // Save the refreshed authentication in the existing session.
            contextRepository.saveContext(context, request, response);
        }

        filterChain.doFilter(request, response);
    }
}