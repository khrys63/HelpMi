package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.AssignOrganizationRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.repository.OrganizationRepository;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks UserService service;

    @Test
    void getActiveUsers_admin_returnsMappedList() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User u1 = adminUser();
        User u2 = agentUser();
        when(userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = service.getActiveUsers();

        assertThat(result).hasSize(2);
    }

    @Test
    void getActiveUsers_agent_returnsMappedList() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());
        when(userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()).thenReturn(List.of(agentUser()));

        assertThatCode(() -> service.getActiveUsers()).doesNotThrowAnyException();
    }

    @Test
    void getActiveUsers_client_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());

        assertThatThrownBy(() -> service.getActiveUsers()).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getCurrentUser_returnsCurrentUserMapped() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        UserResponse result = service.getCurrentUser();

        assertThat(result.email()).isEqualTo("admin@test.com");
        assertThat(result.role()).isEqualTo(admin.getRole());
    }

    @Test
    void getCurrentUser_includesOrganizationFields() {
        User client = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(client);

        UserResponse result = service.getCurrentUser();

        assertThat(result.organizationId()).isNull();
        assertThat(result.organizationName()).isNull();
    }

    // --- getAllUsersForAdmin ---

    @Test
    void getAllUsersForAdmin_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());
        assertThatThrownBy(() -> service.getAllUsersForAdmin()).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAllUsersForAdmin_admin_returnsAll() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(userRepository.findAllByOrderByFirstNameAscLastNameAsc())
                .thenReturn(List.of(adminUser(), agentUser(), clientUser()));

        assertThat(service.getAllUsersForAdmin()).hasSize(3);
    }

    // --- updateUser ---

    @Test
    void updateUser_self_throws() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> service.updateUser(admin.getId(), new UpdateUserRequest(UserRole.CLIENT, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateUser_changesRole() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.updateUser(target.getId(), new UpdateUserRequest(UserRole.AGENT, null));

        assertThat(target.getRole()).isEqualTo(UserRole.AGENT);
    }

    @Test
    void updateUser_deactivates() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = agentUser();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.updateUser(target.getId(), new UpdateUserRequest(null, false));

        assertThat(target.isActive()).isFalse();
    }

    // --- assignOrganization ---

    @Test
    void assignOrganization_adminTarget_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = adminUser();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.assignOrganization(target.getId(), new AssignOrganizationRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignOrganization_nullOrgId_removesOrg() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        Organization org = organization();
        target.setOrganization(org);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.assignOrganization(target.getId(), new AssignOrganizationRequest(null));

        assertThat(target.getOrganization()).isNull();
    }

    @Test
    void assignOrganization_withOrgId_setsOrg() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        Organization org = organization();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.save(target)).thenReturn(target);

        service.assignOrganization(target.getId(), new AssignOrganizationRequest(org.getId()));

        assertThat(target.getOrganization()).isEqualTo(org);
    }
}
