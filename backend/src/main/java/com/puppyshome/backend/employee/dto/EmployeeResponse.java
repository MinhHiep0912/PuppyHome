package com.puppyshome.backend.employee.dto;

import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String displayName,
        String workStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
