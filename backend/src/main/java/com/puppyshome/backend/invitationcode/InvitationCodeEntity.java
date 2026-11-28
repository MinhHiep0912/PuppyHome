package com.puppyshome.backend.invitationcode;

import com.puppyshome.backend.employee.EmployeeProfileEntity;
import com.puppyshome.backend.gameaccount.GameAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_codes")
public class InvitationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_account_id", nullable = false)
    private GameAccountEntity gameAccount;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "note", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_employee_id")
    private EmployeeProfileEntity usedByEmployee;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected InvitationCodeEntity() {
    }

    public InvitationCodeEntity(GameAccountEntity gameAccount, String code, String note) {
        this.gameAccount = gameAccount;
        this.code = code;
        this.note = note;
    }

    public void update(String code, String note) {
        this.code = code;
        this.note = note;
    }

    public void softDelete(LocalDateTime timestamp) {
        this.deletedAt = timestamp;
    }

    public void markUsed(EmployeeProfileEntity employee, LocalDateTime timestamp) {
        this.usedByEmployee = employee;
        this.usedAt = timestamp;
    }

    public Long getId() {
        return id;
    }

    public GameAccountEntity getGameAccount() {
        return gameAccount;
    }

    public String getCode() {
        return code;
    }

    public String getNote() {
        return note;
    }

    public EmployeeProfileEntity getUsedByEmployee() {
        return usedByEmployee;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
