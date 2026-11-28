package com.puppyshome.backend.task.dto;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        Long id,
        Long taskId,
        Long actorId,
        String actorDisplayName,
        String action,
        Long fromAssigneeId,
        String fromAssigneeDisplayName,
        Long toAssigneeId,
        String toAssigneeDisplayName,
        String reason,
        LocalDateTime createdAt) {
}
