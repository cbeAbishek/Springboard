package com.example.automatedtestingframework.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.example.automatedtestingframework.config.ClerkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class ClerkTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(ClerkTokenVerifier.class);

    private final ClerkProperties clerkProperties;
    private final JwkProvider jwkProvider;

    public ClerkTokenVerifier(ClerkProperties clerkProperties) {
        this.clerkProperties = clerkProperties;
        this.jwkProvider = buildProvider(clerkProperties.getJwksUrl());
    }

    private JwkProvider buildProvider(String jwksUrl) {
        if (!StringUtils.hasText(jwksUrl)) {
            return null;
        }
        try {
            URL url = URI.create(jwksUrl).toURL();
            return new JwkProviderBuilder(url)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();
        } catch (IllegalArgumentException | MalformedURLException ex) {
            log.error("Invalid Clerk JWKS url configured: {}", jwksUrl, ex);
            return null;
        }
    }

    public Optional<DecodedJWT> verify(String token) {
        if (!clerkProperties.isEnabled() || !StringUtils.hasText(token) || jwkProvider == null) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(clerkProperties.getIssuer())) {
            return Optional.empty();
        }

        try {
            DecodedJWT decoded = JWT.decode(token);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            Verification verification = JWT.require(algorithm)
                .withIssuer(clerkProperties.getIssuer());
            if (StringUtils.hasText(clerkProperties.getAudience())) {
                verification = verification.withAudience(clerkProperties.getAudience());
            }
            JWTVerifier verifier = verification
                .acceptLeeway(Duration.ofSeconds(10).toSeconds())
                .build();
            return Optional.of(verifier.verify(decoded));
        } catch (JWTVerificationException | JwkException ex) {
            log.debug("Clerk token verification failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
