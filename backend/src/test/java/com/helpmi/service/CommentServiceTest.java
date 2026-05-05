package com.helpmi.service;

import com.helpmi.domain.Comment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreateCommentRequest;
import com.helpmi.dto.response.CommentResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.CommentRepository;
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
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ProjectService projectService;

    @InjectMocks CommentService service;

    private Comment buildComment(Ticket ticket, User author) {
        return Comment.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .author(author)
                .body("Test comment")
                .edited(false)
                .build();
    }

    // --- getComments ---

    @Test
    void getComments_returnsMappedList() {
        User author = clientUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()))
                .thenReturn(List.of(comment));

        List<CommentResponse> result = service.getComments(ticket.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).body()).isEqualTo("Test comment");
    }

    // --- addComment ---

    @Test
    void addComment_ticketNotFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addComment(UUID.randomUUID(), new CreateCommentRequest("body")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addComment_persistsWithCurrentUser() {
        User author = clientUser();
        Ticket ticket = ticket(project(), author);
        when(currentUserService.getCurrentUser()).thenReturn(author);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        Comment saved = buildComment(ticket, author);
        when(commentRepository.save(any())).thenReturn(saved);

        CommentResponse result = service.addComment(ticket.getId(), new CreateCommentRequest("body"));

        assertThat(result.body()).isEqualTo("Test comment");
        verify(commentRepository).save(argThat(c ->
                c.getBody().equals("body") && c.getAuthor().equals(author)));
    }

    // --- updateComment ---

    @Test
    void updateComment_notFound_throws() {
        when(commentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateComment(UUID.randomUUID(), new CreateCommentRequest("x")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateComment_ownComment_succeeds() {
        User author = clientUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(author);
        when(commentRepository.save(comment)).thenReturn(comment);

        service.updateComment(comment.getId(), new CreateCommentRequest("updated body"));

        assertThat(comment.getBody()).isEqualTo("updated body");
        assertThat(comment.isEdited()).isTrue();
    }

    @Test
    void updateComment_otherUserNotAdmin_throws() {
        User author = clientUser();
        User other = clientUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> service.updateComment(comment.getId(), new CreateCommentRequest("x")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateComment_adminCanEditAnyComment() {
        User author = clientUser();
        User admin = adminUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(commentRepository.save(comment)).thenReturn(comment);

        service.updateComment(comment.getId(), new CreateCommentRequest("admin edit"));

        assertThat(comment.getBody()).isEqualTo("admin edit");
    }

    // --- deleteComment ---

    @Test
    void deleteComment_ownComment_deletes() {
        User author = clientUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(author);

        service.deleteComment(comment.getId());

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_otherUserNotAdmin_throws() {
        User author = clientUser();
        User other = clientUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> service.deleteComment(comment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteComment_adminCanDeleteAnyComment() {
        User author = clientUser();
        User admin = adminUser();
        Ticket ticket = ticket(project(), author);
        Comment comment = buildComment(ticket, author);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        service.deleteComment(comment.getId());

        verify(commentRepository).delete(comment);
    }
}
