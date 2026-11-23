package com.puppyshome.backend.gameaccount;

import com.puppyshome.backend.gameaccount.dto.GameAccountResponse;
import com.puppyshome.backend.gameaccount.dto.GameAccountStatusRequest;
import com.puppyshome.backend.gameaccount.dto.GameAccountUpsertRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/game-accounts")
public class GameAccountController {

    private final GameAccountService gameAccountService;

    public GameAccountController(GameAccountService gameAccountService) {
        this.gameAccountService = gameAccountService;
    }

    @GetMapping
    public ResponseEntity<List<GameAccountResponse>> getGameAccounts(
            Authentication authentication) {
        return ResponseEntity.ok(
                gameAccountService.getGameAccounts(authentication));
    }

    @PostMapping
    public ResponseEntity<GameAccountResponse> createGameAccount(
            Authentication authentication,
            @Valid @RequestBody GameAccountUpsertRequest request) {
        GameAccountResponse account =
                gameAccountService.createGameAccount(authentication, request);

        return ResponseEntity
                .created(URI.create("/api/game-accounts/" + account.id()))
                .body(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameAccountResponse> getGameAccount(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                gameAccountService.getGameAccount(authentication, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameAccountResponse> updateGameAccount(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody GameAccountUpsertRequest request) {
        return ResponseEntity.ok(
                gameAccountService.updateGameAccount(
                        authentication, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveGameAccount(
            Authentication authentication,
            @PathVariable Long id) {
        gameAccountService.archiveGameAccount(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GameAccountResponse> updateGameAccountStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody GameAccountStatusRequest request) {
        return ResponseEntity.ok(
                gameAccountService.updateGameAccountStatus(
                        authentication, id, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dữ liệu Game Account không hợp lệ.");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail);
        problem.setTitle("VALIDATION_ERROR");

        return ResponseEntity.badRequest().body(problem);
    }
}
