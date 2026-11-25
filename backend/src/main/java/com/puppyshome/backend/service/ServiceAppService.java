
package com.puppyshome.backend.service;

import com.puppyshome.backend.service.dto.AdminServiceResponse;
import com.puppyshome.backend.service.dto.PublicServiceResponse;
import com.puppyshome.backend.service.dto.ServiceUpsertRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServiceAppService {

    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String DRAFT_STATUS = "DRAFT";

    private final ServiceRepository serviceRepository;

    public ServiceAppService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminServiceResponse> getAllServices() {
        return serviceRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminServiceResponse createService(ServiceUpsertRequest request) {
        if (serviceRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Service slug đã tồn tại.");
        }

        ServiceEntity service = new ServiceEntity(
                request.name(),
                request.slug(),
                request.shortDescription(),
                request.description(),
                request.imageUrl(),
                DRAFT_STATUS,
                request.sortOrder(),
                request.priceAmount(),
                currencyOrDefault(request.priceCurrency()),
                request.priceUnit());

        return toAdminResponse(serviceRepository.saveAndFlush(service));
    }

    @Transactional
    public AdminServiceResponse updateService(
            Long id,
            ServiceUpsertRequest request) {
        ServiceEntity service = getRequiredService(id);

        if (serviceRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Service slug đã tồn tại.");
        }

        service.updateDetails(
                request.name(),
                request.slug(),
                request.shortDescription(),
                request.description(),
                request.imageUrl(),
                request.sortOrder(),
                request.priceAmount(),
                currencyOrDefault(request.priceCurrency()),
                request.priceUnit());

        return toAdminResponse(serviceRepository.saveAndFlush(service));
    }

    @Transactional
    public AdminServiceResponse updateServiceStatus(Long id, String status) {
        ServiceEntity service = getRequiredService(id);
        service.updateStatus(status);
        return toAdminResponse(serviceRepository.saveAndFlush(service));
    }

    @Transactional
    public void softDeleteService(Long id) {
        ServiceEntity service = getRequiredService(id);
        service.updateStatus(DRAFT_STATUS);
        serviceRepository.saveAndFlush(service);
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
                        service.getSortOrder(),
                        service.getPriceAmount(),
                        service.getPriceCurrency(),
                        service.getPriceUnit()
                ))
                .toList();
    }

    private ServiceEntity getRequiredService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Service."));
    }

    private AdminServiceResponse toAdminResponse(ServiceEntity service) {
        return new AdminServiceResponse(
                service.getId(),
                service.getName(),
                service.getSlug(),
                service.getShortDescription(),
                service.getDescription(),
                service.getImageUrl(),
                service.getStatus(),
                service.getSortOrder(),
                service.getPriceAmount(),
                service.getPriceCurrency(),
                service.getPriceUnit());
    }

    private String currencyOrDefault(String currency) {
        return currency == null ? "VND" : currency;
    }
}
