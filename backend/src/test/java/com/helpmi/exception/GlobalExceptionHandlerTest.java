package com.helpmi.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── NotFoundException ─────────────────────────────────────────────────────

    @Test
    void handleNotFound_returns404WithMessage() {
        ProblemDetail pd = handler.handleNotFound(new NotFoundException("Ticket introuvable"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).isEqualTo("Ticket introuvable");
    }

    @Test
    void handleNotFound_noStackTraceInDetail() {
        ProblemDetail pd = handler.handleNotFound(new NotFoundException("Not found"));

        assertThat(pd.getDetail()).doesNotContain("at com.helpmi");
        assertThat(pd.getProperties()).isNullOrEmpty();
    }

    // ── ForbiddenException ────────────────────────────────────────────────────

    @Test
    void handleForbidden_returns403WithMessage() {
        ProblemDetail pd = handler.handleForbidden(new ForbiddenException("Accès refusé"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(pd.getDetail()).isEqualTo("Accès refusé");
    }

    @Test
    void handleForbidden_noStackTraceInDetail() {
        ProblemDetail pd = handler.handleForbidden(new ForbiddenException("Forbidden"));

        assertThat(pd.getDetail()).doesNotContain("at com.helpmi");
    }

    // ── IllegalArgumentException ──────────────────────────────────────────────

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        ProblemDetail pd = handler.handleIllegalArgument(new IllegalArgumentException("Paramètre invalide"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("Paramètre invalide");
    }

    // ── MethodArgumentNotValidException ───────────────────────────────────────

    @Test
    void handleValidation_returns400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "ne doit pas être vide"));
        bindingResult.addError(new FieldError("target", "expiresAt", "doit être dans le futur"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).contains("name: ne doit pas être vide");
        assertThat(pd.getDetail()).contains("expiresAt: doit être dans le futur");
    }

    @Test
    void handleValidation_multipleErrors_joinedWithComma() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "field1", "erreur1"));
        bindingResult.addError(new FieldError("target", "field2", "erreur2"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getDetail()).contains(", ");
    }

    @Test
    void handleValidation_noStackTraceInDetail() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "invalide"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getDetail()).doesNotContain("at com.helpmi");
        assertThat(pd.getProperties()).isNullOrEmpty();
    }
}
