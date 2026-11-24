package com.puppyshome.backend.employee;

import com.puppyshome.backend.employee.dto.EmployeeCreateRequest;
import com.puppyshome.backend.employee.dto.EmployeeResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        return employeeRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "EMPLOYEE_CODE_EXISTS");
        }

        String workStatus = request.workStatus() == null
                ? EmployeeEntity.STATUS_ACTIVE
                : request.workStatus();
        EmployeeEntity employee = new EmployeeEntity(
                request.employeeCode().trim(),
                request.displayName().trim(),
                workStatus);
        employeeRepository.saveAndFlush(employee);
        entityManager.refresh(employee);
        return toResponse(employee);
    }

    private EmployeeResponse toResponse(EmployeeEntity employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getDisplayName(),
                employee.getWorkStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
