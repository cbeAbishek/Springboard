package com.example.automatedtestingframework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ClerkAuthenticationFilter extends OncePerRequestFilter {

    private final ClerkTokenVerifier tokenVerifier;
    private final ClerkUserSynchronizer userSynchronizer;

    public ClerkAuthenticationFilter(ClerkTokenVerifier tokenVerifier,
                                     ClerkUserSynchronizer userSynchronizer) {
        this.tokenVerifier = tokenVerifier;
        this.userSynchronizer = userSynchronizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                .flatMap(tokenVerifier::verify)
                .map(userSynchronizer::synchronize)
                .ifPresent(user -> {
                    AuthenticatedUser principal = new AuthenticatedUser(user);
                    UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                });
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return Optional.of(authorization.substring(7));
        }
        String clerkSession = request.getHeader("Clerk-Session");
        if (StringUtils.hasText(clerkSession)) {
            return Optional.of(clerkSession);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("__session".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }
}
