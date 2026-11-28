package com.puppyshome.backend.employee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeProfileRepository
        extends JpaRepository<EmployeeProfileEntity, Long> {

    List<EmployeeProfileEntity> findAllByOrderByUser_IdAsc();

    boolean existsByEmployeeCode(String employeeCode);
}
