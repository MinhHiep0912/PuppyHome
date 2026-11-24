package com.puppyshome.backend.invitationcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationCodeUpsertRequest(
        @NotBlank(message = "Mã mời không được để trống.")
        @Size(max = 128, message = "Mã mời không được vượt quá 128 ký tự.")
        String code,

        @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự.")
        String note) {
}
