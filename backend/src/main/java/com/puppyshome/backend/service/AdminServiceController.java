package com.puppyshome.backend.service;

import com.puppyshome.backend.service.dto.AdminServiceResponse;
import com.puppyshome.backend.service.dto.ServiceStatusRequest;
import com.puppyshome.backend.service.dto.ServiceUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/admin/services")
public class AdminServiceController {

    private final ServiceAppService serviceAppService;

    public AdminServiceController(ServiceAppService serviceAppService) {
        this.serviceAppService = serviceAppService;
    }

    @GetMapping
    public ResponseEntity<List<AdminServiceResponse>> getAllServices() {
        return ResponseEntity.ok(serviceAppService.getAllServices());
    }

    @PostMapping
    public ResponseEntity<AdminServiceResponse> createService(
            @Valid @RequestBody ServiceUpsertRequest request) {
        AdminServiceResponse service =
                serviceAppService.createService(request);

        return ResponseEntity
                .created(URI.create("/api/admin/services/" + service.id()))
                .body(service);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceUpsertRequest request) {
        return ResponseEntity.ok(
                serviceAppService.updateService(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminServiceResponse> updateServiceStatus(
            @PathVariable Long id,
            @Valid @RequestBody ServiceStatusRequest request) {
        return ResponseEntity.ok(
                serviceAppService.updateServiceStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteService(@PathVariable Long id) {
        serviceAppService.softDeleteService(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataConflict() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Slug đã tồn tại hoặc dữ liệu Service xung đột.");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
