package com.helpmi.service;

import com.helpmi.domain.User;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks UserService service;

    @Test
    void getActiveUsers_returnsMappedList() {
        User u1 = adminUser();
        User u2 = agentUser();
        when(userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = service.getActiveUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("admin@test.com");
        assertThat(result.get(1).email()).isEqualTo("agent@test.com");
    }

    @Test
    void getCurrentUser_returnsCurrentUserMapped() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        UserResponse result = service.getCurrentUser();

        assertThat(result.email()).isEqualTo("admin@test.com");
        assertThat(result.role()).isEqualTo(admin.getRole());
    }
}
