
package com.puppyshome.backend.service;

import com.puppyshome.backend.service.dto.PublicServiceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceAppService {

    private static final String PUBLISHED_STATUS = "PUBLISHED";

    private final ServiceRepository serviceRepository;

    public ServiceAppService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<PublicServiceResponse> getPublishedServices() {
        return serviceRepository
                .findAllByStatusOrderBySortOrderAscIdAsc(PUBLISHED_STATUS)
                .stream()
                .map(service -> new PublicServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getSlug(),
                        service.getShortDescription(),
                        service.getDescription(),
                        service.getImageUrl(),
                        service.getSortOrder()
                ))
                .toList();
    }
}
