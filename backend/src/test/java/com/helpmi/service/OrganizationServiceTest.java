package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.Project;
import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateOrganizationRequest;
import com.helpmi.dto.request.UpdateOrganizationRequest;
import com.helpmi.dto.response.OrganizationResponse;
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
class OrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock AuditService auditService;

    @InjectMocks OrganizationService service;

    // --- listAll ---

    @Test
    void listAll_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());
        assertThatThrownBy(() -> service.listAll()).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listAll_admin_returnsOrgs() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(org));
        when(userRepository.findByOrganizationId(org.getId())).thenReturn(List.of());

        List<OrganizationResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Test Organisation");
    }

    // --- get ---

    @Test
    void get_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void get_notFound_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_happy_returnsOrg() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findByOrganizationId(org.getId())).thenReturn(List.of());

        OrganizationResponse result = service.get(org.getId());

        assertThat(result.name()).isEqualTo("Test Organisation");
        assertThat(result.id()).isEqualTo(org.getId());
    }

    // --- create ---

    @Test
    void create_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());
        assertThatThrownBy(() -> service.create(new CreateOrganizationRequest("Acme")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void create_admin_savesAndReturns() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization saved = organization();
        when(organizationRepository.save(any())).thenReturn(saved);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        OrganizationResponse result = service.create(new CreateOrganizationRequest("  Acme  "));

        verify(organizationRepository).save(argThat(o -> o.getName().equals("Acme")));
        assertThat(result).isNotNull();
    }

    // --- update ---

    @Test
    void update_partialName_onlyChangesName() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.update(org.getId(), new UpdateOrganizationRequest("New Name", null));

        assertThat(org.getName()).isEqualTo("New Name");
        assertThat(org.isActive()).isTrue();
    }

    @Test
    void update_setInactive_deactivates() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.update(org.getId(), new UpdateOrganizationRequest(null, false));

        assertThat(org.isActive()).isFalse();
    }

    // --- delete ---

    @Test
    void delete_withUsers_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationId(org.getId())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(org.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2");
    }

    @Test
    void delete_noUsers_setsInactive() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationId(org.getId())).thenReturn(0L);
        when(organizationRepository.save(org)).thenReturn(org);

        service.delete(org.getId());

        assertThat(org.isActive()).isFalse();
    }

    // --- addProject / removeProject ---

    @Test
    void addProject_addsToCollection() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        Project project = project();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(organizationRepository.save(org)).thenReturn(org);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.addProject(org.getId(), project.getId());

        assertThat(org.getProjects()).contains(project);
    }

    @Test
    void removeProject_removesFromCollection() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        Project project = project();
        org.getProjects().add(project);
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.removeProject(org.getId(), project.getId());

        assertThat(org.getProjects()).doesNotContain(project);
    }

    @Test
    void addProject_projectNotFound_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addProject(org.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // --- addUserToOrganization ---

    @Test
    void addUserToOrganization_admin_succeeds() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User targetAdmin = adminUser();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of(targetAdmin));

        service.addUserToOrganization(org.getId(), targetAdmin.getId());

        assertThat(targetAdmin.getOrganizations()).contains(org);
    }

    @Test
    void addUserToOrganization_nonAdmin_addsOrgToSet() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of(agent));

        service.addUserToOrganization(org.getId(), agent.getId());

        assertThat(agent.getOrganizations()).contains(org);
    }

    // --- removeUserFromOrganization ---

    @Test
    void removeUserFromOrganization_notInOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser(); // no orgs in set
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.removeUserFromOrganization(org.getId(), agent.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeUserFromOrganization_removesOrgFromSet() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUserWithOrg(org);
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of());
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.removeUserFromOrganization(org.getId(), agent.getId());

        assertThat(agent.getOrganizations()).doesNotContain(org);
    }

    @Test
    void removeUserFromOrganization_clearsProjectsFromThatOrg() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUserWithOrg(org);
        Project p = project();
        agent.getUserProjects().add(UserProject.builder()
                .id(UUID.randomUUID()).user(agent).project(p).role("MANAGER").build());
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(projectRepository.findIdsByOrganizationId(org.getId())).thenReturn(List.of(p.getId()));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.removeUserFromOrganization(org.getId(), agent.getId());

        assertThat(agent.getUserProjects()).isEmpty();
    }
}
