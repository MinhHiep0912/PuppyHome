package com.puppyshome.backend.task;

import com.puppyshome.backend.auth.UserEntity;
import com.puppyshome.backend.servicerequest.ServiceRequestEntity;
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
@Table(name = "tasks")
public class TaskEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequestEntity serviceRequest;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private UserEntity currentAssignee;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected TaskEntity() {
    }

    TaskEntity(
            ServiceRequestEntity serviceRequest,
            String title,
            String description,
            LocalDateTime deadlineAt,
            LocalDateTime createdAt) {
        this.serviceRequest = serviceRequest;
        this.title = title;
        this.description = description;
        this.status = STATUS_OPEN;
        this.deadlineAt = deadlineAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void claimBy(UserEntity assignee) {
        this.currentAssignee = assignee;
        this.status = STATUS_IN_PROGRESS;
    }

    void release() {
        this.currentAssignee = null;
        this.status = STATUS_OPEN;
    }

    void reassignTo(UserEntity assignee) {
        this.currentAssignee = assignee;
        if (STATUS_OPEN.equals(status)) {
            this.status = STATUS_IN_PROGRESS;
        }
    }

    void markDone() {
        this.status = STATUS_DONE;
    }

    void markCancelled() {
        this.status = STATUS_CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public ServiceRequestEntity getServiceRequest() {
        return serviceRequest;
    }

    public Long getServiceRequestId() {
        return serviceRequest.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public UserEntity getCurrentAssignee() {
        return currentAssignee;
    }

    public Long getCurrentAssigneeId() {
        return currentAssignee == null ? null : currentAssignee.getId();
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
