package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService extends UserDetailsService {

    User register(User user);

    User findByEmail(String email);

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    User getCurrentUser();

    /** Provjera da je resurs vlasništvo trenutnog korisnika. */
    void assertOwnership(Long ownerId);

    void delete(Long id);

    User save(User currentUser);

    User getById(Long id);

    /** Ažurira profil (username/email) bez promjene lozinke. */
    User updateProfile(String username, String email);
}
