package com.puppyshome.backend.invitationcode;

import com.puppyshome.backend.invitationcode.dto.InvitationCodeResponse;
import com.puppyshome.backend.invitationcode.dto.InvitationCodeUpsertRequest;
import com.puppyshome.backend.invitationcode.dto.MarkUsedRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
public class InvitationCodeController {

    private final InvitationCodeService invitationCodeService;

    public InvitationCodeController(InvitationCodeService invitationCodeService) {
        this.invitationCodeService = invitationCodeService;
    }

    @GetMapping("/api/game-accounts/{id}/invitation-codes")
    public List<InvitationCodeResponse> getInvitationCodes(
            Authentication authentication,
            @PathVariable Long id) {
        return invitationCodeService.getInvitationCodes(authentication, id);
    }

    @PostMapping("/api/game-accounts/{id}/invitation-codes")
    public ResponseEntity<InvitationCodeResponse> createInvitationCode(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody InvitationCodeUpsertRequest request) {
        InvitationCodeResponse code =
                invitationCodeService.createInvitationCode(authentication, id, request);
        return ResponseEntity
                .created(URI.create("/api/invitation-codes/" + code.id()))
                .body(code);
    }

    @PutMapping("/api/invitation-codes/{codeId}")
    public InvitationCodeResponse updateInvitationCode(
            Authentication authentication,
            @PathVariable Long codeId,
            @Valid @RequestBody InvitationCodeUpsertRequest request) {
        return invitationCodeService.updateInvitationCode(authentication, codeId, request);
    }

    @DeleteMapping("/api/invitation-codes/{codeId}")
    public ResponseEntity<Void> deleteInvitationCode(
            Authentication authentication,
            @PathVariable Long codeId) {
        invitationCodeService.deleteInvitationCode(authentication, codeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/invitation-codes/{codeId}/mark-used")
    public InvitationCodeResponse markUsed(
            Authentication authentication,
            @PathVariable Long codeId,
            @Valid @RequestBody MarkUsedRequest request) {
        return invitationCodeService.markUsed(
                authentication, codeId, request.employeeId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dữ liệu Invitation Code không hợp lệ.");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getReason());
        String reason = exception.getReason();
        if ("ACCOUNT_NOT_ACTIVE".equals(reason)
                || "CODE_ALREADY_USED".equals(reason)) {
            problem.setTitle(reason);
        } else {
            problem.setTitle(status.getReasonPhrase());
        }
        return ResponseEntity.status(status).body(problem);
    }
}
