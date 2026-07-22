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

/** Promjena lozinke i reset preko tokena. */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final int TOKEN_HOURS_VALID = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = userService.getCurrentUser();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new CustomException("Stara lozinka nije ispravna.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CustomException("Nova lozinka mora biti različita od stare.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Reset token se vraća u odgovoru (umjesto emaila). */
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Ako nalog sa tim emailom postoji, generisan je reset token.");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return response;
        }

        User user = userOpt.get();
        tokenRepository.deleteByUser(user);

        String tokenValue = generateToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_HOURS_VALID))
                .used(false)
                .build();

        tokenRepository.save(token);

        response.put("resetToken", tokenValue);
        response.put("expiresInHours", String.valueOf(TOKEN_HOURS_VALID));
        return response;
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new CustomException("Reset token nije validan."));

        if (token.isUsed()) {
            throw new CustomException("Reset token je već iskorišten.");
        }

        if (token.isExpired()) {
            throw new CustomException("Reset token je istekao. Zatražite novi.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
