package com.puppyshome.backend.servicerequest;

import com.puppyshome.backend.servicerequest.dto.ServiceRequestCreateRequest;
import com.puppyshome.backend.servicerequest.dto.ServiceRequestResponse;
import com.puppyshome.backend.servicerequest.dto.ServiceRequestStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(
            ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> createServiceRequest(
            Authentication authentication,
            @Valid @RequestBody ServiceRequestCreateRequest request) {
        ServiceRequestResponse created =
                serviceRequestService.createServiceRequest(authentication, request);
        return ResponseEntity
                .created(URI.create("/api/service-requests/" + created.id()))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> getServiceRequests(
            Authentication authentication) {
        return ResponseEntity.ok(
                serviceRequestService.getServiceRequests(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getServiceRequest(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                serviceRequestService.getServiceRequest(authentication, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ServiceRequestResponse> cancelServiceRequest(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                serviceRequestService.cancelServiceRequest(authentication, id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponse> updateServiceRequestStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestStatusRequest request) {
        return ResponseEntity.ok(
                serviceRequestService.updateServiceRequestStatus(
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
                .orElse("Dữ liệu Service Request không hợp lệ.");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail);
        problem.setTitle("VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu Service Request không hợp lệ.");
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
        String detail = exception.getReason();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        if ("ACCOUNT_NOT_ACTIVE".equals(detail)
                || "SERVICE_NOT_AVAILABLE".equals(detail)
                || "PRICE_NOT_AVAILABLE".equals(detail)
                || "INVALID_STATUS_TRANSITION".equals(detail)) {
            problem.setTitle(detail);
        } else {
            problem.setTitle(status.getReasonPhrase());
        }
        return ResponseEntity.status(status).body(problem);
    }
}
