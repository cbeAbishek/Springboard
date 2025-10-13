package com.example.automatedtestingframework.config;

import com.example.automatedtestingframework.security.ClerkAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration using Clerk as the primary authentication provider.
 * This configuration removes traditional form-based login and uses JWT tokens from Clerk.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClerkAuthenticationFilter clerkAuthenticationFilter) throws Exception {
        http
            // Disable CSRF for API endpoints (Clerk uses JWT tokens)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            
            // Configure session management - stateless for JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // Public pages
                .requestMatchers("/", "/landing", "/docs", "/documentation").permitAll()
                .requestMatchers("/signin", "/sign-in", "/register", "/login").permitAll()
                
                // Clerk webhook endpoints (if needed in future)
                .requestMatchers("/api/webhooks/clerk").permitAll()
                
                // Public API endpoints
                .requestMatchers("/api/public/**", "/api/run/**").permitAll()
                
                // Health check / actuator
                .requestMatchers("/actuator/health").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add Clerk authentication filter
            .addFilterBefore(clerkAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling - redirect to landing page for unauthorized
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // For API requests, return 401
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.setHeader("X-Auth-Error", authException != null ? authException.getClass().getSimpleName() : "AuthenticationException");
                        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Please authenticate with Clerk\"}");
                    } else {
                        // For web requests, redirect to sign-in page
                        response.sendRedirect("/signin");
                    }
                })
            );
            
        return http.build();
    }

    /**
     * Password encoder bean - kept for backward compatibility with existing user records
     * Can be removed once fully migrated to Clerk
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
