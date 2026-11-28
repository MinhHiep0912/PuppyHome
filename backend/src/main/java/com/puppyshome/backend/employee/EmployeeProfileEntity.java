package com.puppyshome.backend.employee;

import com.puppyshome.backend.auth.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_profiles")
public class EmployeeProfileEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "employee_code", nullable = false, length = 64)
    private String employeeCode;

    @Column(name = "work_status", nullable = false, length = 32)
    private String workStatus;

    @Column(name = "skill_group", length = 64)
    private String skillGroup;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected EmployeeProfileEntity() {
    }

    public EmployeeProfileEntity(UserEntity user, String employeeCode, String workStatus) {
        this.user = user;
        this.employeeCode = employeeCode;
        this.workStatus = workStatus;
    }

    public void updateWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public Long getId() {
        return user.getId();
    }

    public UserEntity getUser() {
        return user;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(workStatus)
                && user.isActive();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
