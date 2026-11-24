package com.puppyshome.backend.employee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    List<EmployeeEntity> findAllByOrderByIdAsc();
    boolean existsByEmployeeCode(String employeeCode);
}
