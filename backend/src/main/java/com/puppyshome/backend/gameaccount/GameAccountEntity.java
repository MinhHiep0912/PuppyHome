package com.puppyshome.backend.gameaccount;

import com.puppyshome.backend.auth.UserEntity;

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
@Table(name = "game_accounts")
public class GameAccountEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "nocape", nullable = false)
    private boolean nocape;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected GameAccountEntity() {
        // Constructor không tham số dành cho JPA.
    }

    GameAccountEntity(
            UserEntity customer,
            String name,
            boolean nocape) {
        this.customer = customer;
        this.name = name;
        this.nocape = nocape;
        this.status = STATUS_ACTIVE;
    }

    void updateDetails(String name, boolean nocape) {
        this.name = name;
        this.nocape = nocape;
    }

    void updateStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customer.getId();
    }

    public String getName() {
        return name;
    }

    public boolean isNocape() {
        return nocape;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
