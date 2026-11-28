package com.puppyshome.backend.employee.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateRequest(
        @Size(max = 128, message = "Tên hiển thị không được vượt quá 128 ký tự.")
        String displayName,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái làm việc không hợp lệ.")
        String workStatus,

        @Size(min = 8, max = 128, message = "Mật khẩu phải từ 8 đến 128 ký tự.")
        String password) {
}
