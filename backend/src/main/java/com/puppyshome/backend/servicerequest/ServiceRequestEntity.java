package com.puppyshome.backend.servicerequest;

import com.puppyshome.backend.auth.UserEntity;
import com.puppyshome.backend.gameaccount.GameAccountEntity;
import com.puppyshome.backend.service.ServiceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
public class ServiceRequestEntity {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_account_id", nullable = false)
    private GameAccountEntity gameAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "snapshot_service_name", nullable = false, length = 150)
    private String snapshotServiceName;

    @Column(name = "snapshot_price_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal snapshotPriceAmount;

    @Column(name = "snapshot_currency", nullable = false, length = 3)
    private String snapshotCurrency;

    @Column(name = "snapshot_price_unit", nullable = false, length = 32)
    private String snapshotPriceUnit;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "status_actor_type", length = 16)
    private String statusActorType;

    @Column(name = "status_actor_id")
    private Long statusActorId;

    @Column(name = "requested_start_at")
    private LocalDateTime requestedStartAt;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceRequestEntity() {
    }

    ServiceRequestEntity(
            UserEntity customer,
            GameAccountEntity gameAccount,
            ServiceEntity service,
            BigDecimal snapshotPriceAmount,
            String snapshotCurrency,
            String snapshotPriceUnit,
            LocalDateTime requestedStartAt,
            String notes,
            LocalDateTime startAt,
            int durationDays) {
        this.customer = customer;
        this.gameAccount = gameAccount;
        this.service = service;
        this.snapshotServiceName = service.getName();
        this.snapshotPriceAmount = snapshotPriceAmount;
        this.snapshotCurrency = snapshotCurrency;
        this.snapshotPriceUnit = snapshotPriceUnit;
        this.status = STATUS_NEW;
        this.requestedStartAt = requestedStartAt;
        this.startAt = startAt;
        this.endAt = startAt.plusDays(durationDays);
        this.durationDays = durationDays;
        this.notes = notes;
        this.createdAt = startAt;
        this.updatedAt = startAt;
    }

    void updateStatus(String status, String actorType, Long actorId) {
        this.status = status;
        this.statusActorType = actorType;
        this.statusActorId = actorId;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customer.getId();
    }

    public Long getGameAccountId() {
        return gameAccount.getId();
    }

    public Long getServiceId() {
        return service.getId();
    }

    public String getSnapshotServiceName() {
        return snapshotServiceName;
    }

    public BigDecimal getSnapshotPriceAmount() {
        return snapshotPriceAmount;
    }

    public String getSnapshotCurrency() {
        return snapshotCurrency;
    }

    public String getSnapshotPriceUnit() {
        return snapshotPriceUnit;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusActorType() {
        return statusActorType;
    }

    public Long getStatusActorId() {
        return statusActorId;
    }

    public LocalDateTime getRequestedStartAt() {
        return requestedStartAt;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
