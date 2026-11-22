package com.puppyshome.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email không được dài quá 254 ký tự.")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(min = 8, max = 128, message = "Mật khẩu phải từ 8 đến 128 ký tự.")
        String password
) {
}
