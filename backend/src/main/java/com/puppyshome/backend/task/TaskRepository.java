package com.puppyshome.backend.task;

import java.util.List;
import java.util.Optional;

import com.puppyshome.backend.auth.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findByServiceRequest_Id(Long serviceRequestId);

    List<TaskEntity> findAllByOrderByIdAsc();

    @Query("""
            SELECT t FROM TaskEntity t
            WHERE (:status IS NULL OR t.status = :status)
            ORDER BY t.id ASC
            """)
    List<TaskEntity> findAllFiltered(@Param("status") String status);

    @Query("""
            SELECT t FROM TaskEntity t
            WHERE (:status IS NULL OR t.status = :status)
              AND (t.currentAssignee.id = :userId
                   OR (t.status = 'OPEN' AND t.currentAssignee IS NULL))
            ORDER BY t.id ASC
            """)
    List<TaskEntity> findVisibleToUser(
            @Param("userId") Long userId,
            @Param("status") String status);

    @Query("""
            SELECT t FROM TaskEntity t
            WHERE t.id = :taskId
              AND (t.currentAssignee.id = :userId
                   OR (t.status = 'OPEN' AND t.currentAssignee IS NULL))
            """)
    Optional<TaskEntity> findVisibleToUserById(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE TaskEntity t
            SET t.currentAssignee = :assignee,
                t.status = 'IN_PROGRESS'
            WHERE t.id = :taskId
              AND t.status = 'OPEN'
              AND t.currentAssignee IS NULL
            """)
    int claimIfUnassigned(
            @Param("taskId") Long taskId,
            @Param("assignee") UserEntity assignee);
}
