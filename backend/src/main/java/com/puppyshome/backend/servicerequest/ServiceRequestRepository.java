package com.puppyshome.backend.servicerequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequestEntity, Long> {

    List<ServiceRequestEntity> findAllByCustomer_IdOrderByIdAsc(Long customerId);

    List<ServiceRequestEntity> findAllByOrderByIdAsc();
}
