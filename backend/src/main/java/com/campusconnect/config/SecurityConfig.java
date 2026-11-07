
package com.campusconnect.config;

import com.campusconnect.auth.CurrentUserSessionFilter;
import com.campusconnect.auth.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Server-managed session authentication with CSRF protection and
 * deny-by-default authorization rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource")
            CorsConfigurationSource cors,
            SecurityContextRepository contextRepository,
            ObjectProvider<UserRepository> usersProvider)
            throws Exception {

        http
            .cors(c -> c.configurationSource(cors))
            .securityContext(c ->
                c.securityContextRepository(contextRepository))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(l -> l
                .logoutUrl("/api/v1/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("CC_SESSION")
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler(
                        HttpStatus.NO_CONTENT)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) ->
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN)))
            .authorizeHttpRequests(a -> a
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/health",
                    "/api/v1/auth/csrf",
                    "/api/v1/public/drives")
                .permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/auth/register")
                .permitAll()
                .requestMatchers(
                    HttpMethod.OPTIONS, "/api/**")
                .permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("PLACEMENT_ADMIN")
                .anyRequest().authenticated());

        // Some slice tests intentionally omit repositories. In those tests,
        // leave the existing test authentication behavior untouched.
        UserRepository users = usersProvider.getIfAvailable();
        if (users != null) {
            CurrentUserSessionFilter filter =
                new CurrentUserSessionFilter(users, contextRepository);

            http.addFilterBefore(
                filter, AnonymousAuthenticationFilter.class);
        }

        return http.build();
    }
}
