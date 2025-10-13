package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

@Controller
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = resolveUser(principal);
        model.addAttribute("user", user);
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setOrganization(user.getOrganization());
            form.setJobTitle(user.getJobTitle());
            form.setAvatarUrl(user.getAvatarUrl());
            model.addAttribute("profileForm", form);
        }
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileForm", result);
            redirectAttributes.addFlashAttribute("profileForm", form);
            redirectAttributes.addFlashAttribute("error", "Please correct the highlighted fields.");
            return "redirect:/profile";
        }

        user.setFullName(form.getFullName());
        user.setOrganization(blankToNull(form.getOrganization()));
        user.setJobTitle(blankToNull(form.getJobTitle()));
        user.setAvatarUrl(blankToNull(form.getAvatarUrl()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "Profile updated successfully.");
        return "redirect:/profile";
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static class ProfileForm {
        @NotBlank
        @Size(max = 80)
        private String fullName;

        @Size(max = 120)
        private String organization;

        @Size(max = 120)
        private String jobTitle;

        @Size(max = 256)
        private String avatarUrl;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
        }
    }
}
