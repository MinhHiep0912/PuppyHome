package com.puppyshome.backend.task;

import com.puppyshome.backend.auth.AccessContext;
import com.puppyshome.backend.auth.AccessContextResolver;
import com.puppyshome.backend.auth.RoleEntity;
import com.puppyshome.backend.auth.UserEntity;
import com.puppyshome.backend.auth.UserRepository;
import com.puppyshome.backend.employee.EmployeeProfileEntity;
import com.puppyshome.backend.employee.EmployeeProfileRepository;
import com.puppyshome.backend.servicerequest.ServiceRequestEntity;
import com.puppyshome.backend.task.dto.TaskHistoryResponse;
import com.puppyshome.backend.task.dto.TaskReassignRequest;
import com.puppyshome.backend.task.dto.TaskResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final AccessContextResolver accessContextResolver;

    @PersistenceContext
    private EntityManager entityManager;

    public TaskService(
            TaskRepository taskRepository,
            TaskAssignmentHistoryRepository historyRepository,
            UserRepository userRepository,
            EmployeeProfileRepository employeeProfileRepository,
            AccessContextResolver accessContextResolver) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.accessContextResolver = accessContextResolver;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Authentication authentication, String status) {
        AccessContext context = accessContextResolver.resolve(authentication);
        List<TaskEntity> tasks;
        if (context.admin()) {
            tasks = taskRepository.findAllFiltered(normalizeStatus(status));
        } else if (context.employee()) {
            tasks = taskRepository.findVisibleToUser(
                    context.userId(), normalizeStatus(status));
        } else {
            throw forbidden();
        }
        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Authentication authentication, Long id) {
        return toResponse(getAccessibleTask(authentication, id));
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> getTaskHistory(
            Authentication authentication,
            Long id) {
        TaskEntity task = getAccessibleTask(authentication, id);
        return historyRepository.findByTask_IdOrderByIdAsc(task.getId())
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public TaskResponse claimTask(Authentication authentication, Long id) {
        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin() && !context.employee()) {
            throw forbidden();
        }
        UserEntity actor = getActorUser(context);
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy Task."));

        if (TaskEntity.STATUS_DONE.equals(task.getStatus())
                || TaskEntity.STATUS_CANCELLED.equals(task.getStatus())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }
        if (task.getCurrentAssignee() != null) {
            throw conflict("TASK_ALREADY_CLAIMED");
        }
        if (!TaskEntity.STATUS_OPEN.equals(task.getStatus())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }

        int updated = taskRepository.claimIfUnassigned(task.getId(), actor);
        if (updated == 0) {
            entityManager.refresh(task);
            if (task.getCurrentAssignee() != null
                    && !task.getCurrentAssignee().getId().equals(actor.getId())) {
                throw conflict("TASK_ALREADY_CLAIMED");
            }
            throw conflict("INVALID_STATUS_TRANSITION");
        }
        entityManager.refresh(task);

        saveHistory(task, actor,
                TaskAssignmentHistoryEntity.ACTION_CLAIM,
                null, task.getCurrentAssignee(), null);
        entityManager.refresh(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse releaseTask(Authentication authentication, Long id) {
        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin() && !context.employee()) {
            throw forbidden();
        }
        UserEntity actor = getActorUser(context);
        TaskEntity task = getVisibleTaskForMutation(context, id);

        if (TaskEntity.STATUS_DONE.equals(task.getStatus())
                || TaskEntity.STATUS_CANCELLED.equals(task.getStatus())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }
        UserEntity previousAssignee = task.getCurrentAssignee();
        if (previousAssignee == null) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }
        if (!context.admin()
                && !previousAssignee.getId().equals(context.userId())) {
            throw notFound("Không tìm thấy Task.");
        }

        saveHistory(task, actor,
                TaskAssignmentHistoryEntity.ACTION_RELEASE,
                previousAssignee, null, null);
        task.release();
        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse reassignTask(
            Authentication authentication,
            Long id,
            TaskReassignRequest request) {
        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin()) {
            throw forbidden();
        }
        UserEntity actor = getActorUser(context);
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy Task."));

        if (TaskEntity.STATUS_DONE.equals(task.getStatus())
                || TaskEntity.STATUS_CANCELLED.equals(task.getStatus())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }

        UserEntity target = userRepository.findById(request.assigneeUserId())
                .orElseThrow(() -> notFound("Không tìm thấy Employee."));
        boolean isEmployee = target.getRoles()
                .stream()
                .anyMatch(role -> RoleEntity.CODE_EMPLOYEE.equals(role.getCode()));
        if (!isEmployee) {
            throw notFound("Không tìm thấy Employee.");
        }
        if (!target.isActive()) {
            throw conflict("EMPLOYEE_NOT_ACTIVE");
        }

        UserEntity previousAssignee = task.getCurrentAssignee();
        saveHistory(task, actor,
                TaskAssignmentHistoryEntity.ACTION_REASSIGN,
                previousAssignee, target,
                normalizeReason(request.reason()));
        task.reassignTo(target);
        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
        return toResponse(task);
    }

    @Transactional
    public void syncFromServiceRequest(ServiceRequestEntity request) {
        Optional<TaskEntity> existing =
                taskRepository.findByServiceRequest_Id(request.getId());
        String status = request.getStatus();

        if (ServiceRequestEntity.STATUS_IN_PROGRESS.equals(status)) {
            if (existing.isEmpty()) {
                TaskEntity task = new TaskEntity(
                        request,
                        request.getSnapshotServiceName(),
                        request.getNotes(),
                        request.getEndAt(),
                        LocalDateTime.now());
                taskRepository.saveAndFlush(task);
                entityManager.refresh(task);
            }
            return;
        }

        if (existing.isEmpty()) {
            return;
        }
        TaskEntity task = existing.get();

        if (ServiceRequestEntity.STATUS_DONE.equals(status)
                && !TaskEntity.STATUS_DONE.equals(task.getStatus())
                && !TaskEntity.STATUS_CANCELLED.equals(task.getStatus())) {
            UserEntity actor = findActor(request.getStatusActorId());
            UserEntity assignee = task.getCurrentAssignee();
            if (actor != null) {
                saveHistory(task, actor,
                        TaskAssignmentHistoryEntity.ACTION_COMPLETE,
                        assignee, assignee, null);
            }
            task.markDone();
            taskRepository.saveAndFlush(task);
            entityManager.refresh(task);
            return;
        }

        if (ServiceRequestEntity.STATUS_CANCELLED.equals(status)
                && !TaskEntity.STATUS_DONE.equals(task.getStatus())
                && !TaskEntity.STATUS_CANCELLED.equals(task.getStatus())) {
            UserEntity actor = findActor(request.getStatusActorId());
            if (actor != null) {
                saveHistory(task, actor,
                        TaskAssignmentHistoryEntity.ACTION_CANCELLED,
                        task.getCurrentAssignee(), null, null);
            }
            task.markCancelled();
            taskRepository.saveAndFlush(task);
            entityManager.refresh(task);
        }
    }

    private TaskEntity getAccessibleTask(
            Authentication authentication,
            Long id) {
        AccessContext context = accessContextResolver.resolve(authentication);
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy Task."));
        if (context.admin()) {
            return task;
        }
        if (context.employee() && isVisibleToEmployee(task, context.userId())) {
            return task;
        }
        if (context.employee()) {
            throw notFound("Không tìm thấy Task.");
        }
        throw forbidden();
    }

    private TaskEntity getVisibleTaskForMutation(
            AccessContext context,
            Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy Task."));
        if (context.admin()) {
            return task;
        }
        if (isVisibleToEmployee(task, context.userId())) {
            return task;
        }
        throw notFound("Không tìm thấy Task.");
    }

    private boolean isVisibleToEmployee(TaskEntity task, Long userId) {
        return (task.getCurrentAssignee() != null
                    && task.getCurrentAssignee().getId().equals(userId))
                || (TaskEntity.STATUS_OPEN.equals(task.getStatus())
                    && task.getCurrentAssignee() == null);
    }

    private UserEntity getActorUser(AccessContext context) {
        UserEntity user = userRepository.findById(context.userId())
                .orElseThrow(() -> notFound("Không tìm thấy người dùng."));
        if (context.employee()
                && !user.isActive()) {
            throw conflict("EMPLOYEE_NOT_ACTIVE");
        }
        return user;
    }

    private UserEntity findActor(Long actorId) {
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElse(null);
    }

    private void saveHistory(
            TaskEntity task,
            UserEntity actor,
            String action,
            UserEntity fromAssignee,
            UserEntity toAssignee,
            String reason) {
        historyRepository.saveAndFlush(new TaskAssignmentHistoryEntity(
                task, actor, action, fromAssignee, toAssignee, reason));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private TaskResponse toResponse(TaskEntity task) {
        UserEntity assignee = task.getCurrentAssignee();
        return new TaskResponse(
                task.getId(),
                task.getServiceRequestId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCurrentAssigneeId(),
                assignee == null ? null : assignee.getDisplayName(),
                task.getDeadlineAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private TaskHistoryResponse toHistoryResponse(
            TaskAssignmentHistoryEntity entry) {
        UserEntity actor = entry.getActor();
        UserEntity from = entry.getFromAssignee();
        UserEntity to = entry.getToAssignee();
        return new TaskHistoryResponse(
                entry.getId(),
                entry.getTaskId(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getDisplayName(),
                entry.getAction(),
                from == null ? null : from.getId(),
                from == null ? null : from.getDisplayName(),
                to == null ? null : to.getId(),
                to == null ? null : to.getDisplayName(),
                entry.getReason(),
                entry.getCreatedAt());
    }

    private ResponseStatusException notFound(String detail) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, detail);
    }

    private ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập.");
    }
}
