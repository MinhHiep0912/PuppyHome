package com.puppyshome.backend.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank(message = "Mã nhân viên không được để trống.")
        @Size(max = 64, message = "Mã nhân viên không được vượt quá 64 ký tự.")
        String employeeCode,

        @NotBlank(message = "Tên hiển thị không được để trống.")
        @Size(max = 128, message = "Tên hiển thị không được vượt quá 128 ký tự.")
        String displayName,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái làm việc không hợp lệ.")
        String workStatus) {
}
