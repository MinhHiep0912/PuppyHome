package com.puppyshome.backend.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssignmentHistoryRepository
        extends JpaRepository<TaskAssignmentHistoryEntity, Long> {

    List<TaskAssignmentHistoryEntity> findByTask_IdOrderByIdAsc(Long taskId);
}
