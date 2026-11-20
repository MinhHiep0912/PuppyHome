package com.puppyshome.backend.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository
        extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findAllByOrderBySortOrderAscIdAsc();

    List<ServiceEntity> findAllByStatusOrderBySortOrderAscIdAsc(
            String status
    );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}