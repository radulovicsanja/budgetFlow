package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.exception.ForbiddenException;
import com.example.budgetFlow.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registracija, login (UserDetails) i trenutni korisnik. */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultCategoryService defaultCategoryService;

    /** Registracija + seed podrazumijevanih kategorija. */
    @Override
    @Transactional
    public User register(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new CustomException("Email already exists.");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new CustomException("Username already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(com.example.budgetFlow.entity.Role.USER);
        }
        User saved = userRepository.save(user);

        // greška seed-a ne smije poništiti registraciju
        try {
            defaultCategoryService.seedForUser(saved);
        } catch (Exception ignored) {
            // ignore
        }

        return saved;
    }

    @Override
    public User findByEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        return userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username)
            throws UsernameNotFoundException {

        String normalized = username.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + username));

        String roleName = user.getRole() != null
                ? user.getRole().name()
                : "USER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(roleName)
                .build();
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assert authentication != null;
        String username = authentication.getName();

        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void assertOwnership(Long ownerId) {
        if (ownerId == null) {
            throw new ForbiddenException("Nemate pristup ovom resursu.");
        }
        User current = getCurrentUser();
        if (!current.getId().equals(ownerId)) {
            throw new ForbiddenException("Nemate pristup tuđim podacima.");
        }
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Korisnik nije pronađen."));
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public User updateProfile(String username, String email) {
        User current = getCurrentUser();

        if (username != null && !username.isBlank()) {
            if (!current.getUsername().equals(username)
                    && userRepository.existsByUsername(username)) {
                throw new CustomException("Username already exists.");
            }
            current.setUsername(username);
        }

        if (email != null && !email.isBlank()) {
            if (!current.getEmail().equals(email)
                    && userRepository.existsByEmail(email)) {
                throw new CustomException("Email already exists.");
            }
            current.setEmail(email);
        }

        return userRepository.save(current);
    }
}
