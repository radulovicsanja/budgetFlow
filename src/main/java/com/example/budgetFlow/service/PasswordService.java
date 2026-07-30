package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.PasswordResetToken;
import com.example.budgetFlow.entity.User;
import com.example.budgetFlow.exception.CustomException;
import com.example.budgetFlow.repository.PasswordResetTokenRepository;
import com.example.budgetFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Promjena lozinke i resetovanje lozinke preko sigurnosnog tokena. */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final int TOKEN_HOURS_VALID = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = userService.getCurrentUser();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new CustomException("Stara lozinka nije ispravna.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CustomException(
                    "Nova lozinka mora biti različita od stare."
            );
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> forgotPassword(String email) {
        Map<String, String> response = new LinkedHashMap<>();

        response.put(
                "message",
                "Ako nalog sa tim emailom postoji, " +
                        "poslat je link za resetovanje lozinke."
        );

        if (email == null || email.isBlank()) {
            return response;
        }

        Optional<User> userOpt = userRepository.findByEmail(
                email.trim().toLowerCase()
        );

        if (userOpt.isEmpty()) {
            return response;
        }

        User user = userOpt.get();

        tokenRepository.deleteAllByUserId(user.getId());

        String tokenValue = generateToken();

        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(
                        LocalDateTime.now().plusHours(TOKEN_HOURS_VALID)
                )
                .used(false)
                .build();

        tokenRepository.saveAndFlush(token);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                tokenValue
        );

        return response;
    }

    @Transactional
    public void resetPassword(
            String tokenValue,
            String newPassword
    ) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new CustomException(
                    "Reset token nije validan."
            );
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new CustomException(
                    "Nova lozinka nije unesena."
            );
        }

        PasswordResetToken token = tokenRepository
                .findByToken(tokenValue.trim())
                .orElseThrow(
                        () -> new CustomException(
                                "Reset token nije validan."
                        )
                );

        if (token.isUsed()) {
            throw new CustomException(
                    "Reset token je već iskorišten."
            );
        }

        if (token.isExpired()) {
            throw new CustomException(
                    "Reset token je istekao. Zatražite novi."
            );
        }

        User user = token.getUser();

        if (passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {
            throw new CustomException(
                    "Nova lozinka mora biti različita od stare."
            );
        }

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.saveAndFlush(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}