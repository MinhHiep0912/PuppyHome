package com.puppyshome.backend.task;

import com.puppyshome.backend.task.dto.TaskHistoryResponse;
import com.puppyshome.backend.task.dto.TaskReassignRequest;
import com.puppyshome.backend.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getTasks(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return taskService.getTasks(authentication, status);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(
            Authentication authentication,
            @PathVariable Long id) {
        return taskService.getTask(authentication, id);
    }

    @GetMapping("/{id}/history")
    public List<TaskHistoryResponse> getTaskHistory(
            Authentication authentication,
            @PathVariable Long id) {
        return taskService.getTaskHistory(authentication, id);
    }

    @PostMapping("/{id}/claim")
    public TaskResponse claimTask(
            Authentication authentication,
            @PathVariable Long id) {
        return taskService.claimTask(authentication, id);
    }

    @PostMapping("/{id}/release")
    public TaskResponse releaseTask(
            Authentication authentication,
            @PathVariable Long id) {
        return taskService.releaseTask(authentication, id);
    }

    @PostMapping("/{id}/reassign")
    public TaskResponse reassignTask(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TaskReassignRequest request) {
        return taskService.reassignTask(authentication, id, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dữ liệu Task không hợp lệ.");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, detail);
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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, exception.getReason());
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}
