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

    // --- setUserOrganization ---

    @Test
    void setUserOrganization_admin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User targetAdmin = adminUser();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() -> service.setUserOrganization(org.getId(), targetAdmin.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setUserOrganization_nonAdmin_assignsOrg() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser();
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of(agent));

        service.setUserOrganization(org.getId(), agent.getId());

        assertThat(agent.getOrganization()).isEqualTo(org);
    }

    @Test
    void setUserOrganization_orgChanges_clearsProjects() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization currentOrg = organization();
        Organization newOrg = organization();
        User agent = agentUser();
        agent.setOrganization(currentOrg);
        agent.getUserProjects().add(UserProject.builder()
                .id(UUID.randomUUID()).user(agent).project(project()).role("UTILISATEUR").build());
        when(organizationRepository.findByIdWithProjects(newOrg.getId())).thenReturn(Optional.of(newOrg));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.setUserOrganization(newOrg.getId(), agent.getId());

        assertThat(agent.getUserProjects()).isEmpty();
        assertThat(agent.getOrganization()).isEqualTo(newOrg);
    }

    @Test
    void setUserOrganization_sameOrg_keepsProjects() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser();
        agent.setOrganization(org);
        agent.getUserProjects().add(UserProject.builder()
                .id(UUID.randomUUID()).user(agent).project(project()).role("GESTIONNAIRE").build());
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of(agent));

        service.setUserOrganization(org.getId(), agent.getId());

        assertThat(agent.getUserProjects()).hasSize(1);
    }

    // --- removeUserFromOrganization ---

    @Test
    void removeUserFromOrganization_wrongOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        Organization otherOrg = organization();
        User agent = agentUser();
        agent.setOrganization(otherOrg);
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.removeUserFromOrganization(org.getId(), agent.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeUserFromOrganization_setsOrgNull() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser();
        agent.setOrganization(org);
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.removeUserFromOrganization(org.getId(), agent.getId());

        assertThat(agent.getOrganization()).isNull();
    }

    @Test
    void removeUserFromOrganization_clearsProjects() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Organization org = organization();
        User agent = agentUser();
        agent.setOrganization(org);
        agent.getUserProjects().add(UserProject.builder()
                .id(UUID.randomUUID()).user(agent).project(project()).role("GESTIONNAIRE").build());
        when(organizationRepository.findByIdWithProjects(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);
        when(userRepository.findByOrganizationId(any())).thenReturn(List.of());

        service.removeUserFromOrganization(org.getId(), agent.getId());

        assertThat(agent.getOrganization()).isNull();
        assertThat(agent.getUserProjects()).isEmpty();
    }
}
