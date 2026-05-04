package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.Project;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreateProjectRequest;
import com.helpmi.dto.request.UpdateProjectRequest;
import com.helpmi.dto.response.ProjectResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.domain.UserProject;
import com.helpmi.repository.ProjectRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.repository.UserProjectRepository;
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
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock TicketRepository ticketRepository;
    @Mock UserProjectRepository userProjectRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks ProjectService service;

    // --- getAllProjects ---

    @Test
    void getAllProjects_admin_withOrg_returnsOnlyMemberProjects() {
        User admin = adminUser();
        admin.getOrganizations().add(organization());
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        Project p = project();
        when(projectRepository.findActiveByUserId(admin.getId())).thenReturn(List.of(p));

        List<ProjectResponse> result = service.getAllProjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Test Project");
        verify(projectRepository, never()).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    void getAllProjects_admin_withoutOrg_returnsEmpty() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.findActiveByUserId(admin.getId())).thenReturn(List.of());

        List<ProjectResponse> result = service.getAllProjects();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllProjects_nonAdminWithOrg_returnsOrgProjects() {
        User agent = agentUser();
        Organization org = organization();
        agent.getOrganizations().add(org);
        when(currentUserService.getCurrentUser()).thenReturn(agent);
        Project p = project();
        when(projectRepository.findActiveByUserId(agent.getId())).thenReturn(List.of(p));

        List<ProjectResponse> result = service.getAllProjects();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllProjects_nonAdminWithoutOrg_returnsEmpty() {
        User agent = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(agent);
        when(projectRepository.findActiveByUserId(agent.getId())).thenReturn(List.of());

        List<ProjectResponse> result = service.getAllProjects();

        assertThat(result).isEmpty();
    }

    // --- getProject ---

    @Test
    void getProject_admin_memberOfProject_succeeds() {
        User admin = adminUser();
        admin.getOrganizations().add(organization());
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), admin.getId())).thenReturn(true);

        assertThatCode(() -> service.getProject(p.getId())).doesNotThrowAnyException();
    }

    @Test
    void getProject_admin_notMemberOfProject_throwsForbidden() {
        User admin = adminUser();
        admin.getOrganizations().add(organization());
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), admin.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getProject(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProject_admin_withoutOrg_throwsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getProject(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProject_nonAdminWithAccess_succeeds() {
        User agent = agentUser();
        Organization org = organization();
        agent.getOrganizations().add(org);
        when(currentUserService.getCurrentUser()).thenReturn(agent);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), agent.getId())).thenReturn(true);

        assertThatCode(() -> service.getProject(p.getId())).doesNotThrowAnyException();
    }

    @Test
    void getProject_nonAdminWithoutAccess_throws() {
        User client = clientUser();
        Organization org = organization();
        client.getOrganizations().add(org);
        when(currentUserService.getCurrentUser()).thenReturn(client);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.getProject(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProject_nonAdminNoOrg_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getProject(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- createProject ---

    @Test
    void createProject_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());

        assertThatThrownBy(() -> service.createProject(new CreateProjectRequest("Name", "KEY", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createProject_duplicateKey_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(projectRepository.existsByKey("EXISTING")).thenReturn(true);

        assertThatThrownBy(() -> service.createProject(new CreateProjectRequest("Name", "existing", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EXISTING");
    }

    @Test
    void createProject_normalizesKeyToUppercase() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.existsByKey("DEMO")).thenReturn(false);
        Project saved = project();
        when(projectRepository.save(any())).thenReturn(saved);

        service.createProject(new CreateProjectRequest("Demo Project", "demo", null));

        verify(projectRepository).save(argThat(p -> p.getKey().equals("DEMO")));
    }

    @Test
    void createProject_setsCurrentUserAsCreator() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.existsByKey(any())).thenReturn(false);
        Project saved = project();
        when(projectRepository.save(any())).thenReturn(saved);

        service.createProject(new CreateProjectRequest("My Project", "MP", null));

        verify(projectRepository).save(argThat(p -> p.getCreatedBy().equals(admin)));
    }

    // --- updateProject ---

    @Test
    void updateProject_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> service.updateProject(UUID.randomUUID(), new UpdateProjectRequest("n", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateProject_partialUpdate_onlyChangesProvidedFields() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.save(p)).thenReturn(p);

        service.updateProject(p.getId(), new UpdateProjectRequest("New Name", null));

        assertThat(p.getName()).isEqualTo("New Name");
        assertThat(p.getDescription()).isNull();
    }

    // --- deleteProject ---

    @Test
    void deleteProject_setsActiveFalse() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        Project p = project();
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.save(p)).thenReturn(p);

        service.deleteProject(p.getId());

        assertThat(p.isActive()).isFalse();
        verify(projectRepository).save(p);
    }

    // --- findActive ---

    @Test
    void findActive_inactiveProject_throws() {
        Project p = project();
        p.setActive(false);
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.findActive(p.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findActive_notFound_throws() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActive(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // --- isGestionnaire ---

    @Test
    void isGestionnaire_userIsManager_returnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UserProject up = UserProject.builder().id(UUID.randomUUID()).role("MANAGER").build();
        when(userProjectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(up));

        assertThat(service.isGestionnaire(userId, projectId)).isTrue();
    }

    @Test
    void isGestionnaire_userIsMember_returnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UserProject up = UserProject.builder().id(UUID.randomUUID()).role("MEMBER").build();
        when(userProjectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(up));

        assertThat(service.isGestionnaire(userId, projectId)).isFalse();
    }

    @Test
    void isGestionnaire_noRelation_returnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(userProjectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.empty());

        assertThat(service.isGestionnaire(userId, projectId)).isFalse();
    }

    // --- requireProjectAccess (UUID overload) ---

    @Test
    void requireProjectAccess_admin_memberOfProject_doesNotThrow() {
        User admin = adminUser();
        admin.getOrganizations().add(organization());
        Project p = project();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), admin.getId())).thenReturn(true);

        assertThatCode(() -> service.requireProjectAccess(p.getId())).doesNotThrowAnyException();
    }

    @Test
    void requireProjectAccess_admin_notMemberOfProject_throwsForbidden() {
        User admin = adminUser();
        admin.getOrganizations().add(organization());
        Project p = project();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), admin.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.requireProjectAccess(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireProjectAccess_nonAdminWithAccess_doesNotThrow() {
        Project p = project();
        User user = agentUserWithOrg(organization());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.isProjectAccessibleToUser(p.getId(), user.getId())).thenReturn(true);

        assertThatCode(() -> service.requireProjectAccess(p.getId())).doesNotThrowAnyException();
    }

    @Test
    void requireProjectAccess_noOrganization_throwsForbidden() {
        Project p = project();
        User user = agentUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(projectRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.requireProjectAccess(p.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- generateTicketReference ---

    @Test
    void generateTicketReference_incrementsSequenceAndFormatsReference() {
        Project p = project();
        p.setKey("DEMO");
        p.setTicketSequence(4);
        when(projectRepository.findByIdForUpdate(p.getId())).thenReturn(Optional.of(p));
        when(projectRepository.save(p)).thenReturn(p);

        String ref = service.generateTicketReference(p.getId());

        assertThat(ref).isEqualTo("DEMO-5");
        assertThat(p.getTicketSequence()).isEqualTo(5);
    }

    @Test
    void generateTicketReference_projectNotFound_throws() {
        when(projectRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateTicketReference(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
