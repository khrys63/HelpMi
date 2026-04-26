package com.helpmi.service;

import com.helpmi.dto.response.UserResponse;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public List<UserResponse> getActiveUsers() {
        return userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                .stream().map(UserResponse::from).toList();
    }

    public UserResponse getCurrentUser() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }
}
