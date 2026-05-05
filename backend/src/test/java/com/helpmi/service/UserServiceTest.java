package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.Project;
import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.UpdateLocaleRequest;
import com.helpmi.dto.request.UpdateNotificationPrefsRequest;
import com.helpmi.dto.request.UpdateThemeRequest;
import com.helpmi.dto.request.UpdateUserProjectsRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.OrganizationRepository;
import com.helpmi.repository.ProjectRepository;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock ProjectRepository projectRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks UserService service;

    // --- getActiveUsers ---

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
    void getActiveUsers_user_doesNotThrow() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());
        when(userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()).thenReturn(List.of(agentUser()));

        assertThatCode(() -> service.getActiveUsers()).doesNotThrowAnyException();
    }

    // --- getCurrentUser ---

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
        User user = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        UserResponse result = service.getCurrentUser();

        assertThat(result.organizations()).isEmpty();
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

    // --- getAssignableUsers ---

    @Test
    void getAssignableUsers_delegatesToRepo() {
        UUID projectId = UUID.randomUUID();
        User user = agentUser();
        when(userRepository.findAssignableByProjectId(projectId)).thenReturn(List.of(user));

        List<UserResponse> result = service.getAssignableUsers(projectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("agent@test.com");
        verify(userRepository).findAssignableByProjectId(projectId);
    }

    // --- updateUser ---

    @Test
    void updateUser_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> service.updateUser(UUID.randomUUID(), new UpdateUserRequest(null, false)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateUser_self_throws() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> service.updateUser(admin.getId(), new UpdateUserRequest(UserRole.USER, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateUser_notFound_throws() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        UUID otherId = UUID.randomUUID();
        when(userRepository.findById(otherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUser(otherId, new UpdateUserRequest(null, false)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUser_changesRole() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.updateUser(target.getId(), new UpdateUserRequest(UserRole.USER, null));

        assertThat(target.getRole()).isEqualTo(UserRole.USER);
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

    // --- addOrganization ---

    @Test
    void addOrganization_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> service.addOrganization(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addOrganization_adminTarget_succeeds() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = adminUser();
        Organization org = organization();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.save(target)).thenReturn(target);

        UserResponse result = service.addOrganization(target.getId(), org.getId());

        assertThat(target.getOrganizations()).contains(org);
    }

    @Test
    void addOrganization_orgNotFound_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addOrganization(target.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addOrganization_addsOrgToSet() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        Organization org = organization();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.save(target)).thenReturn(target);

        service.addOrganization(target.getId(), org.getId());

        assertThat(target.getOrganizations()).contains(org);
    }

    // --- removeOrganization ---

    @Test
    void removeOrganization_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> service.removeOrganization(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeOrganization_userNotInOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser(); // no orgs
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.removeOrganization(target.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeOrganization_removesOrgAndOrgProjects() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User target = agentUserWithOrg(org);
        Project p = project();
        target.getUserProjects().add(UserProject.builder()
                .id(UUID.randomUUID()).user(target).project(p).role("MEMBER").build());
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of(p.getId()));
        when(userRepository.save(target)).thenReturn(target);

        service.removeOrganization(target.getId(), org.getId());

        assertThat(target.getOrganizations()).doesNotContain(org);
        assertThat(target.getUserProjects()).isEmpty();
    }

    // --- updateUserProjects ---

    @Test
    void updateUserProjects_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> service.updateUserProjects(UUID.randomUUID(),
                new UpdateUserProjectsRequest(List.of())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateUserProjects_userNoOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User user = agentUser(); // no org
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateUserProjects(user.getId(),
                new UpdateUserProjectsRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organisation");
    }

    @Test
    void updateUserProjects_projectNotInOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User user = agentUserWithOrg(org);
        UUID allowedId = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of(allowedId));

        var entries = List.of(new UpdateUserProjectsRequest.ProjectRoleEntry(foreignId, "MEMBER"));
        assertThatThrownBy(() -> service.updateUserProjects(user.getId(),
                new UpdateUserProjectsRequest(entries)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("autorisé");
    }

    @Test
    void updateUserProjects_happy_setsGestionnaireRole() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User user = agentUserWithOrg(org);
        Project p = project();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of(p.getId()));
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(userRepository.save(user)).thenReturn(user);

        var entries = List.of(new UpdateUserProjectsRequest.ProjectRoleEntry(p.getId(), "MANAGER"));
        UserResponse result = service.updateUserProjects(user.getId(), new UpdateUserProjectsRequest(entries));

        assertThat(result.projectRoles()).hasSize(1);
        assertThat(result.projectRoles().get(0).role()).isEqualTo("MANAGER");
        assertThat(result.projectRoles().get(0).projectId()).isEqualTo(p.getId());
    }

    @Test
    void updateUserProjects_nullRole_defaultsToUtilisateur() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User user = agentUserWithOrg(org);
        Project p = project();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of(p.getId()));
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(userRepository.save(user)).thenReturn(user);

        var entries = List.of(new UpdateUserProjectsRequest.ProjectRoleEntry(p.getId(), null));
        UserResponse result = service.updateUserProjects(user.getId(), new UpdateUserProjectsRequest(entries));

        assertThat(result.projectRoles().get(0).role()).isEqualTo("MEMBER");
    }

    // --- updateTheme / updateLocale / updateNotificationPrefs ---

    @Test
    void updateTheme_persistsTheme() {
        User user = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        service.updateTheme(new UpdateThemeRequest("dark"));

        assertThat(user.getTheme()).isEqualTo("dark");
        verify(userRepository).save(user);
    }

    @Test
    void updateLocale_persistsLocale() {
        User user = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        service.updateLocale(new UpdateLocaleRequest("en"));

        assertThat(user.getLocale()).isEqualTo("en");
        verify(userRepository).save(user);
    }

    @Test
    void updateNotificationPrefs_persistsAllPrefs() {
        User user = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        service.updateNotificationPrefs(new UpdateNotificationPrefsRequest(false, false, false, false, false));

        assertThat(user.isNotifAssigned()).isFalse();
        assertThat(user.isNotifComment()).isFalse();
        assertThat(user.isNotifStatusChanged()).isFalse();
        assertThat(user.isNotifWatcherAdded()).isFalse();
        assertThat(user.isNotifTicketCreated()).isFalse();
    }

    // --- addOrganization (branch manquant) ---

    @Test
    void addOrganization_inactiveOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        User target = clientUser();
        Organization inactiveOrg = Organization.builder()
                .id(UUID.randomUUID()).name("Inactive").active(false).build();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(organizationRepository.findById(inactiveOrg.getId())).thenReturn(Optional.of(inactiveOrg));

        assertThatThrownBy(() -> service.addOrganization(target.getId(), inactiveOrg.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUserProjects_emptyEntries_clearsAllProjects() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User user = agentUserWithOrg(org);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of());
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = service.updateUserProjects(user.getId(), new UpdateUserProjectsRequest(List.of()));

        assertThat(result.projectRoles()).isEmpty();
    }
}
