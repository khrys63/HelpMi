package com.helpmi.service;

import com.helpmi.domain.Comment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateCommentRequest;
import com.helpmi.dto.response.CommentResponse;
import com.helpmi.dto.response.UserSummary;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.CommentRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    public CommentResponse addComment(UUID ticketId, CreateCommentRequest req) {
        Ticket ticket = findTicket(ticketId);
        requireEditable(ticket);
        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(currentUserService.getCurrentUser())
                .body(req.body())
                .build();
        return toResponse(commentRepository.save(comment));
    }

    public CommentResponse updateComment(UUID commentId, CreateCommentRequest req) {
        Comment comment = findComment(commentId);
        requireEditable(comment.getTicket());
        User currentUser = currentUserService.getCurrentUser();
        if (!comment.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres commentaires");
        }
        comment.setBody(req.body());
        comment.setEdited(true);
        return toResponse(commentRepository.save(comment));
    }

    public void deleteComment(UUID commentId) {
        Comment comment = findComment(commentId);
        requireEditable(comment.getTicket());
        User currentUser = currentUserService.getCurrentUser();
        if (!comment.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres commentaires");
        }
        commentRepository.delete(comment);
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new NotFoundException("Ticket introuvable"));
    }

    private static void requireEditable(Ticket ticket) {
        if ("CLOSED".equals(ticket.getStatus()) || "CANCELLED".equals(ticket.getStatus())) {
            throw new ForbiddenException("Ce ticket est clôturé et ne peut plus être modifié");
        }
    }

    private Comment findComment(UUID id) {
        return commentRepository.findById(id).orElseThrow(() -> new NotFoundException("Commentaire introuvable"));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(c.getId(), c.getTicket().getId(), UserSummary.from(c.getAuthor()),
                c.getBody(), c.isEdited(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
