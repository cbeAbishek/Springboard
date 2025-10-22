package com.example.automatedtestingframework.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClerkUserSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(ClerkUserSynchronizer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ClerkUserSynchronizer(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User synchronize(DecodedJWT jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaim("email").asString();
        String firstName = jwt.getClaim("first_name").asString();
        String lastName = jwt.getClaim("last_name").asString();
        String fullName = buildFullName(firstName, lastName, email);
        String avatarUrl = jwt.getClaim("profile_image_url").asString();
        Map<String, Object> metadata = Map.of();
        if (!jwt.getClaim("public_metadata").isNull()) {
            try {
                Map<String, Object> raw = jwt.getClaim("public_metadata").asMap();
                metadata = raw != null ? raw : Map.of();
            } catch (Exception ex) {
                log.debug("Failed to parse Clerk public metadata: {}", ex.getMessage());
                metadata = Map.of();
            }
        }
        String organization = valueOrNull(metadata.get("organization"));
        String jobTitle = valueOrNull(metadata.get("jobTitle"));

        Optional<User> existing = Optional.empty();
        if (StringUtils.hasText(clerkUserId)) {
            existing = userRepository.findByClerkUserId(clerkUserId);
        }
        if (existing.isEmpty() && StringUtils.hasText(email)) {
            existing = userRepository.findByEmail(email);
        }

        User user = existing.map(stored -> updateUser(stored, clerkUserId, email, fullName, avatarUrl, organization, jobTitle))
            .orElseGet(() -> createUser(clerkUserId, email, fullName, avatarUrl, organization, jobTitle));

        // No longer automatically creating a default project - users will be redirected to project setup
        return user;
    }

    private User updateUser(User user,
                            String clerkUserId,
                            String email,
                            String fullName,
                            String avatarUrl,
                            String organization,
                            String jobTitle) {
        if (StringUtils.hasText(clerkUserId)) {
            user.setClerkUserId(clerkUserId);
        }
        if (StringUtils.hasText(email)) {
            user.setEmail(email.toLowerCase(Locale.US));
        }
        if (StringUtils.hasText(fullName)) {
            user.setFullName(fullName);
        }
        if (StringUtils.hasText(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        }
        if (StringUtils.hasText(organization)) {
            user.setOrganization(organization);
        }
        if (StringUtils.hasText(jobTitle)) {
            user.setJobTitle(jobTitle);
        }
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private User createUser(String clerkUserId,
                            String email,
                            String fullName,
                            String avatarUrl,
                            String organization,
                            String jobTitle) {
        User user = new User();
        user.setClerkUserId(clerkUserId);
    String resolvedEmail = email != null ? email.toLowerCase(Locale.US) : sanitizeFallbackEmail(clerkUserId);
    user.setEmail(resolvedEmail);
        user.setFullName(fullName != null ? fullName : "Automation User");
        user.setAvatarUrl(avatarUrl);
        user.setOrganization(organization);
        user.setJobTitle(jobTitle);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setRole("USER");
        User saved = userRepository.save(user);
        log.info("Provisioned new user from Clerk identity: {}", saved.getEmail());
        return saved;
    }

    private String buildFullName(String firstName, String lastName, String email) {
        if (StringUtils.hasText(firstName) || StringUtils.hasText(lastName)) {
            return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        }
        if (StringUtils.hasText(email)) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.substring(0, atIndex);
            }
            return email;
        }
        return "Automation User";
    }

    private String sanitizeFallbackEmail(String clerkUserId) {
        if (!StringUtils.hasText(clerkUserId)) {
            return UUID.randomUUID().toString().replaceAll("-", "") + "@users.local";
        }
        String sanitized = clerkUserId.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        return (sanitized.isBlank() ? UUID.randomUUID().toString().replaceAll("-", "") : sanitized)
            + "@users.local";
    }

    private String valueOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        return str.isBlank() ? null : str;
    }
}
