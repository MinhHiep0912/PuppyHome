package com.puppyshome.backend.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class EmployeeEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, length = 64)
    private String employeeCode;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "work_status", nullable = false, length = 32)
    private String workStatus;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected EmployeeEntity() {
    }

    public EmployeeEntity(String employeeCode, String displayName, String workStatus) {
        this.employeeCode = employeeCode;
        this.displayName = displayName;
        this.workStatus = workStatus;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
