package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService extends UserDetailsService {

    User register(User user);

    User findByEmail(String email);

    // Metod za Spring Security JWT autentifikaciju
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;


    User getCurrentUser();

    void delete(Long id);

    User save(User currentUser);
    User getById(Long id);
}
