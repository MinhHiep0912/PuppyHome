package com.puppyshome.backend.auth;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String STATUS_LOCKED = "LOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_identifier", nullable = false, length = 254)
    private String loginIdentifier;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected UserEntity() {
    }

    public UserEntity(
            String loginIdentifier,
            String passwordHash,
            String displayName,
            String status) {
        this.loginIdentifier = loginIdentifier;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
    }

    public void updateProfile(String displayName, String passwordHash) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (passwordHash != null) {
            this.passwordHash = passwordHash;
        }
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
