package com.mos.user.service;

import com.mos.common.exception.InvalidCredentialsException;
import com.mos.security.MosUserPrincipal;
import com.mos.security.SecurityUtils;
import com.mos.user.dto.UserResponse;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        MosUserPrincipal principal = SecurityUtils.getCurrentUser();

        return userRepository.findById(principal.userId())
                .map(UserResponse::from)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
