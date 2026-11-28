package com.puppyshome.backend.task.dto;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        Long serviceRequestId,
        String title,
        String description,
        String status,
        Long currentAssigneeId,
        String assigneeDisplayName,
        LocalDateTime deadlineAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
