package com.helpmi.controller;

import com.helpmi.domain.enums.UserRole;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.security.CurrentUserService;
import com.helpmi.service.LabelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.helpmi.Fixtures.adminUser;
import static com.helpmi.Fixtures.agentUser;
import static com.helpmi.Fixtures.clientUser;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLabelControllerTest {

    @Mock LabelService labelService;
    @Mock CurrentUserService currentUserService;

    @InjectMocks AdminLabelController controller;

    // ── find-or-create : vérification du rôle (H2) ───────────────────────────

    @Test
    void findOrCreate_adminUser_callsService() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(labelService.findOrCreate(any())).thenReturn(null);

        controller.findOrCreate(Map.of("name", "bug"));

        verify(labelService).findOrCreate("bug");
    }

    @Test
    void findOrCreate_clientUser_throwsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());

        assertThatThrownBy(() -> controller.findOrCreate(Map.of("name", "bug")))
                .isInstanceOf(ForbiddenException.class);
        verify(labelService, never()).findOrCreate(any());
    }

    @Test
    void findOrCreate_agentUser_throwsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser());

        assertThatThrownBy(() -> controller.findOrCreate(Map.of("name", "bug")))
                .isInstanceOf(ForbiddenException.class);
        verify(labelService, never()).findOrCreate(any());
    }

    @Test
    void findOrCreate_missingNameKey_usesEmptyString() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(labelService.findOrCreate(any())).thenReturn(null);

        controller.findOrCreate(Map.of());

        verify(labelService).findOrCreate("");
    }
}
