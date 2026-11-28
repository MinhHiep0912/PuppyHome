-- V9: Task Management (F08/E05) — tasks + task_assignment_history.
-- Quyết định MT19b (2026-11-28):
--   * assignee/actor FK -> users.id (sau unified auth V8);
--   * 1 service_request = 1 task (service_request_id UNIQUE);
--   * task tạo tự động khi request NEW->IN_PROGRESS; status DONE/CANCELLED
--     đồng bộ một chiều từ service_requests;
--   * claim chống race bằng conditional UPDATE
--     (WHERE current_assignee_id IS NULL AND status = 'OPEN').

CREATE TABLE tasks (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    service_request_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    current_assignee_id BIGINT UNSIGNED NULL,
    deadline_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_tasks_service_request UNIQUE (service_request_id),
    CONSTRAINT fk_tasks_service_request
        FOREIGN KEY (service_request_id)
        REFERENCES service_requests (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (current_assignee_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_tasks_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    INDEX idx_tasks_status_assignee (status, current_assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_assignment_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_id BIGINT UNSIGNED NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_assignee_id BIGINT UNSIGNED NULL,
    to_assignee_id BIGINT UNSIGNED NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_task_history_task
        FOREIGN KEY (task_id)
        REFERENCES tasks (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task_history_actor
        FOREIGN KEY (actor_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task_history_from_assignee
        FOREIGN KEY (from_assignee_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task_history_to_assignee
        FOREIGN KEY (to_assignee_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_task_history_action
        CHECK (action IN (
            'CLAIM', 'RELEASE', 'REASSIGN', 'COMPLETE', 'CANCELLED')),
    INDEX idx_task_history_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
