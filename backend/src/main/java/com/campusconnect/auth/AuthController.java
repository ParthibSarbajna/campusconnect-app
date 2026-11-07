package com.campusconnect.auth;

import com.campusconnect.account.AccountService;
import com.campusconnect.account.Dtos.CsrfResponse;
import com.campusconnect.account.Dtos.LoginRequest;
import com.campusconnect.account.Dtos.RegisterRequest;
import com.campusconnect.account.Dtos.UserResponse;
import com.campusconnect.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Logout is handled by Spring Security's LogoutFilter at POST /api/v1/auth/logout (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final AccountService accounts;
    private final SessionLoginHelper sessions;

    public AuthController(AuthService auth, AccountService accounts, SessionLoginHelper sessions) {
        this.auth = auth;
        this.accounts = accounts;
        this.sessions = sessions;
    }

    /** The SPA calls this once, then sends the token back in the named header on POST/PUT/PATCH/DELETE. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(auth.registerStudent(request));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserAccount account = auth.authenticate(request.email(), request.password());
        sessions.establish(AuthUser.from(account), httpRequest, httpResponse);
        return UserResponse.from(account);
    }

    /** Returns fresh data from the database; a deleted or disabled account ends the session. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser principal, HttpServletRequest request) {
        return accounts.findActive(principal.id())
                .map(UserResponse::from)
                .orElseThrow(() -> {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    return new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in.");
                });
    }
}
