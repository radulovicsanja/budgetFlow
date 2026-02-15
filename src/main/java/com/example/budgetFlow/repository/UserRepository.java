package com.example.budgetFlow.repository;

import com.example.budgetFlow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Pronalaženje korisnika po email-u (za login)
    Optional<User> findByEmail(String email);

    // Provjera da li email već postoji (registracija)
    boolean existsByEmail(String email);

    // Provjera da li username već postoji
    boolean existsByUsername(String username);
}
