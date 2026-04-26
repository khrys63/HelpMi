package com.helpmi.controller;

import com.helpmi.dto.request.CreateCommentRequest;
import com.helpmi.dto.response.CommentResponse;
import com.helpmi.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/tickets/{ticketId}/comments")
    public List<CommentResponse> list(@PathVariable UUID ticketId) {
        return commentService.getComments(ticketId);
    }

    @PostMapping("/api/tickets/{ticketId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable UUID ticketId, @Valid @RequestBody CreateCommentRequest req) {
        return commentService.addComment(ticketId, req);
    }

    @PutMapping("/api/comments/{commentId}")
    public CommentResponse update(@PathVariable UUID commentId, @Valid @RequestBody CreateCommentRequest req) {
        return commentService.updateComment(commentId, req);
    }

    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
    }
}
