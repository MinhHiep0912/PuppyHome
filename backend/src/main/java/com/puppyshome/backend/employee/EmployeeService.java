package com.puppyshome.backend.employee;

import com.puppyshome.backend.auth.RoleEntity;
import com.puppyshome.backend.auth.RoleRepository;
import com.puppyshome.backend.auth.UserEntity;
import com.puppyshome.backend.auth.UserRepository;
import com.puppyshome.backend.employee.dto.EmployeeCreateRequest;
import com.puppyshome.backend.employee.dto.EmployeeResponse;
import com.puppyshome.backend.employee.dto.EmployeeUpdateRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeService(
            EmployeeProfileRepository employeeProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeProfileRepository = employeeProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        return employeeProfileRepository.findAllByOrderByUser_IdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        String employeeCode = request.employeeCode().trim();
        if (employeeProfileRepository.existsByEmployeeCode(employeeCode)
                || userRepository.existsByLoginIdentifier(employeeCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "EMPLOYEE_CODE_EXISTS");
        }

        String workStatus = request.workStatus() == null
                ? EmployeeProfileEntity.STATUS_ACTIVE
                : request.workStatus();
        RoleEntity employeeRole = roleRepository
                .findByCode(RoleEntity.CODE_EMPLOYEE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Thiếu role EMPLOYEE."));

        UserEntity user = new UserEntity(
                employeeCode,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                workStatusToUserStatus(workStatus));
        user.getRoles().add(employeeRole);
        userRepository.saveAndFlush(user);

        EmployeeProfileEntity employee = new EmployeeProfileEntity(
                user,
                employeeCode,
                workStatus);
        employeeProfileRepository.saveAndFlush(employee);
        entityManager.refresh(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long userId, EmployeeUpdateRequest request) {
        EmployeeProfileEntity employee = employeeProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Employee."));
        UserEntity user = employee.getUser();

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.updateProfile(request.displayName().trim(), null);
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.updateProfile(null, passwordEncoder.encode(request.password()));
        }
        if (request.workStatus() != null) {
            employee.updateWorkStatus(request.workStatus());
            user.updateStatus(workStatusToUserStatus(request.workStatus()));
        }

        userRepository.saveAndFlush(user);
        employeeProfileRepository.saveAndFlush(employee);
        entityManager.refresh(employee);
        return toResponse(employee);
    }

    private String workStatusToUserStatus(String workStatus) {
        return EmployeeProfileEntity.STATUS_ACTIVE.equals(workStatus)
                ? UserEntity.STATUS_ACTIVE
                : UserEntity.STATUS_DISABLED;
    }

    private EmployeeResponse toResponse(EmployeeProfileEntity employee) {
        UserEntity user = employee.getUser();
        return new EmployeeResponse(
                user.getId(),
                user.getLoginIdentifier(),
                employee.getEmployeeCode(),
                user.getDisplayName(),
                employee.getWorkStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
