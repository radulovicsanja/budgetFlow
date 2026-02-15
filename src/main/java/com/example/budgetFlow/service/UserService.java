package com.example.budgetFlow.service;

import com.example.budgetFlow.entity.User;

public interface UserService {

    User register(User user);

    User findByEmail(String email);
}
