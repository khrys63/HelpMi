package com.helpmi.service;

import com.helpmi.domain.Client;
import com.helpmi.dto.request.ClientRequest;
import com.helpmi.dto.response.ClientResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;

    @InjectMocks ClientService service;

    // --- findAll ---

    @Test
    void findAll_returnsMappedList() {
        Client c1 = client("Acme");
        Client c2 = client("Beta");
        when(clientRepository.findAllByOrderByNameAsc()).thenReturn(List.of(c1, c2));

        List<ClientResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Acme");
        assertThat(result.get(1).name()).isEqualTo("Beta");
    }

    // --- create ---

    @Test
    void create_persistsAndReturnsResponse() {
        ClientRequest req = new ClientRequest("  Acme Corp  ", "contact@acme.com", true);
        Client saved = client("Acme Corp");
        when(clientRepository.save(any())).thenReturn(saved);

        ClientResponse result = service.create(req);

        assertThat(result.name()).isEqualTo("Acme Corp");
        verify(clientRepository).save(argThat(c -> c.getName().equals("Acme Corp")
                && c.getContactEmail().equals("contact@acme.com")));
    }

    @Test
    void create_trimsName() {
        ClientRequest req = new ClientRequest("  Spaces  ", null, true);
        Client saved = client("Spaces");
        when(clientRepository.save(any())).thenReturn(saved);

        service.create(req);

        verify(clientRepository).save(argThat(c -> c.getName().equals("Spaces")));
    }

    // --- update ---

    @Test
    void update_notFound_throws() {
        when(clientRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), new ClientRequest("X", null, true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_updatesFields() {
        Client existing = client("OldName");
        when(clientRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        service.update(existing.getId(), new ClientRequest("NewName", "new@mail.com", false));

        assertThat(existing.getName()).isEqualTo("NewName");
        assertThat(existing.getContactEmail()).isEqualTo("new@mail.com");
        assertThat(existing.isActive()).isFalse();
    }

    // --- delete ---

    @Test
    void delete_notFound_throws() {
        when(clientRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_inUse_throws() {
        Client c = client("UsedClient");
        when(clientRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(clientRepository.countTicketsByClientId(c.getId())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(c.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2");
    }

    @Test
    void delete_notInUse_deletesEntity() {
        Client c = client("FreeClient");
        when(clientRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(clientRepository.countTicketsByClientId(c.getId())).thenReturn(0L);

        service.delete(c.getId());

        verify(clientRepository).delete(c);
    }
}
