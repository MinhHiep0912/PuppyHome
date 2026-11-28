package com.puppyshome.backend.task;

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
@Table(name = "task_assignment_history")
public class TaskAssignmentHistoryEntity {

    public static final String ACTION_CLAIM = "CLAIM";
    public static final String ACTION_RELEASE = "RELEASE";
    public static final String ACTION_REASSIGN = "REASSIGN";
    public static final String ACTION_COMPLETE = "COMPLETE";
    public static final String ACTION_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserEntity actor;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_assignee_id")
    private UserEntity fromAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_assignee_id")
    private UserEntity toAssignee;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TaskAssignmentHistoryEntity() {
    }

    TaskAssignmentHistoryEntity(
            TaskEntity task,
            UserEntity actor,
            String action,
            UserEntity fromAssignee,
            UserEntity toAssignee,
            String reason) {
        this.task = task;
        this.actor = actor;
        this.action = action;
        this.fromAssignee = fromAssignee;
        this.toAssignee = toAssignee;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public Long getTaskId() {
        return task.getId();
    }

    public UserEntity getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public UserEntity getFromAssignee() {
        return fromAssignee;
    }

    public UserEntity getToAssignee() {
        return toAssignee;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
