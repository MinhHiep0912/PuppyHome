package com.puppyshome.backend.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskReassignRequest(
        @NotNull(message = "Người nhận không được để trống.")
        Long assigneeUserId,

        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason) {
}
