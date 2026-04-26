package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.TicketLink;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreateTicketLinkRequest;
import com.helpmi.dto.response.TicketLinkResponse;
import com.helpmi.dto.response.TicketSummary;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.TicketLinkRepository;
import com.helpmi.repository.TicketRepository;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketLinkServiceTest {

    @Mock TicketLinkRepository linkRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks TicketLinkService service;

    private TicketLink buildLink(Ticket source, Ticket target, String type) {
        return TicketLink.builder()
                .id(UUID.randomUUID())
                .sourceTicket(source)
                .targetTicket(target)
                .linkType(type)
                .build();
    }

    // --- createLink ---

    @Test
    void createLink_selfLink_throws() {
        User user = agentUser();
        Ticket ticket = ticket(project(), user);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        CreateTicketLinkRequest req = new CreateTicketLinkRequest(ticket.getId(), "BLOCKS");

        assertThatThrownBy(() -> service.createLink(ticket.getId(), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lui-même");
    }

    @Test
    void createLink_duplicateLink_throws() {
        User user = agentUser();
        Ticket source = ticket(project(), user);
        Ticket target = ticket(project(), user);
        when(ticketRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(ticketRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(linkRepository.existsBySourceTicketIdAndTargetTicketIdAndLinkType(
                source.getId(), target.getId(), "BLOCKS")).thenReturn(true);

        assertThatThrownBy(() -> service.createLink(source.getId(),
                new CreateTicketLinkRequest(target.getId(), "BLOCKS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void createLink_valid_returnsOutgoingResponse() {
        User user = agentUser();
        Ticket source = ticket(project(), user);
        source.setReference("TEST-1");
        Ticket target = ticket(project(), user);
        target.setReference("TEST-2");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(ticketRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(ticketRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(linkRepository.existsBySourceTicketIdAndTargetTicketIdAndLinkType(
                source.getId(), target.getId(), "DEPENDS_ON")).thenReturn(false);
        TicketLink saved = buildLink(source, target, "DEPENDS_ON");
        when(linkRepository.save(any())).thenReturn(saved);

        TicketLinkResponse result = service.createLink(source.getId(),
                new CreateTicketLinkRequest(target.getId(), "DEPENDS_ON"));

        assertThat(result.direction()).isEqualTo("OUTGOING");
        assertThat(result.linkType()).isEqualTo("DEPENDS_ON");
    }

    @Test
    void createLink_sourceNotFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLink(UUID.randomUUID(),
                new CreateTicketLinkRequest(UUID.randomUUID(), "BLOCKS")))
                .isInstanceOf(NotFoundException.class);
    }

    // --- deleteLink ---

    @Test
    void deleteLink_notFound_throws() {
        when(linkRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLink(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteLink_found_deletes() {
        User user = agentUser();
        Ticket source = ticket(project(), user);
        Ticket target = ticket(project(), user);
        TicketLink link = buildLink(source, target, "RELATES_TO");
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        service.deleteLink(link.getId());

        verify(linkRepository).delete(link);
    }

    // --- search ---

    @Test
    void search_queryTooShort_returnsEmpty() {
        List<TicketSummary> result = service.search("a", UUID.randomUUID());

        assertThat(result).isEmpty();
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void search_nullQuery_returnsEmpty() {
        List<TicketSummary> result = service.search(null, UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void search_validQuery_excludesCurrentTicket() {
        User user = agentUser();
        Ticket t1 = ticket(project(), user);
        Ticket t2 = ticket(project(), user);
        UUID excludeId = t1.getId();
        when(ticketRepository.searchByQuery(any(), any())).thenReturn(List.of(t1, t2));

        List<TicketSummary> result = service.search("TEST", excludeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(t2.getId());
    }
}
