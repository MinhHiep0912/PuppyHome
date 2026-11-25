package com.puppyshome.backend.servicerequest;

import com.puppyshome.backend.auth.AccessContext;
import com.puppyshome.backend.auth.AccessContextResolver;
import com.puppyshome.backend.auth.AdminRepository;
import com.puppyshome.backend.auth.CustomerEntity;
import com.puppyshome.backend.auth.CustomerRepository;
import com.puppyshome.backend.gameaccount.GameAccountEntity;
import com.puppyshome.backend.gameaccount.GameAccountRepository;
import com.puppyshome.backend.service.ServiceEntity;
import com.puppyshome.backend.service.ServiceRepository;
import com.puppyshome.backend.servicerequest.dto.ServiceRequestCreateRequest;
import com.puppyshome.backend.servicerequest.dto.ServiceRequestResponse;
import com.puppyshome.backend.servicerequest.dto.ServiceRequestStatusRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ServiceRequestService {

    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final int DEFAULT_DURATION_DAYS = 30;
    private static final ZoneId APPLICATION_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final ServiceRequestRepository serviceRequestRepository;
    private final CustomerRepository customerRepository;
    private final GameAccountRepository gameAccountRepository;
    private final ServiceRepository serviceRepository;
    private final AdminRepository adminRepository;
    private final AccessContextResolver accessContextResolver;

    @PersistenceContext
    private EntityManager entityManager;

    public ServiceRequestService(
            ServiceRequestRepository serviceRequestRepository,
            CustomerRepository customerRepository,
            GameAccountRepository gameAccountRepository,
            ServiceRepository serviceRepository,
            AdminRepository adminRepository,
            AccessContextResolver accessContextResolver) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerRepository = customerRepository;
        this.gameAccountRepository = gameAccountRepository;
        this.serviceRepository = serviceRepository;
        this.adminRepository = adminRepository;
        this.accessContextResolver = accessContextResolver;
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getServiceRequests(
            Authentication authentication) {
        AccessContext context = accessContextResolver.resolve(authentication);
        List<ServiceRequestEntity> requests = context.admin()
                ? serviceRequestRepository.findAllByOrderByIdAsc()
                : serviceRequestRepository
                        .findAllByCustomer_IdOrderByIdAsc(context.customerId());
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getServiceRequest(
            Authentication authentication,
            Long id) {
        return toResponse(getRequiredAccessibleRequest(authentication, id));
    }

    @Transactional
    public ServiceRequestResponse createServiceRequest(
            Authentication authentication,
            ServiceRequestCreateRequest request) {
        AccessContext context = accessContextResolver.resolve(authentication);
        CustomerEntity customer = customerRepository
                .findById(context.customerId())
                .orElseThrow(() -> notFound("Không tìm thấy Customer."));

        GameAccountEntity account = gameAccountRepository
                .findById(request.gameAccountId())
                .orElseThrow(() -> notFound("Không tìm thấy Game Account."));
        if (!account.getCustomerId().equals(customer.getId())) {
            throw notFound("Không tìm thấy Game Account.");
        }
        if (!GameAccountEntity.STATUS_ACTIVE.equals(account.getStatus())) {
            throw conflict("ACCOUNT_NOT_ACTIVE");
        }

        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> notFound("Không tìm thấy Service."));
        if (!PUBLISHED_STATUS.equals(service.getStatus())) {
            throw conflict("SERVICE_NOT_AVAILABLE");
        }
        if (service.getPriceAmount() == null) {
            throw conflict("PRICE_NOT_AVAILABLE");
        }

        LocalDateTime startAt = nowInApplicationZone();
        ServiceRequestEntity entity = new ServiceRequestEntity(
                customer,
                account,
                service,
                service.getPriceAmount(),
                service.getPriceCurrency() == null
                        ? "VND"
                        : service.getPriceCurrency(),
                service.getPriceUnit() == null ? "" : service.getPriceUnit(),
                request.requestedStartAt(),
                normalizeNotes(request.notes()),
                startAt,
                DEFAULT_DURATION_DAYS);

        serviceRequestRepository.saveAndFlush(entity);
        entityManager.refresh(entity);
        return toResponse(entity);
    }

    @Transactional
    public ServiceRequestResponse cancelServiceRequest(
            Authentication authentication,
            Long id) {
        ServiceRequestEntity request =
                getRequiredAccessibleRequest(authentication, id);
        if (!ServiceRequestEntity.STATUS_NEW.equals(request.getStatus())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }
        request.updateStatus(
                ServiceRequestEntity.STATUS_CANCELLED,
                "CUSTOMER",
                request.getCustomerId());
        serviceRequestRepository.saveAndFlush(request);
        entityManager.refresh(request);
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse updateServiceRequestStatus(
            Authentication authentication,
            Long id,
            ServiceRequestStatusRequest statusRequest) {
        ServiceRequestEntity request =
                serviceRequestRepository.findById(id)
                        .orElseThrow(() -> notFound(
                                "Không tìm thấy Service Request."));

        if (!isAllowedTransition(request.getStatus(), statusRequest.status())) {
            throw conflict("INVALID_STATUS_TRANSITION");
        }

        Long adminId = adminRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> notFound("Không tìm thấy Admin."))
                .getId();
        request.updateStatus(statusRequest.status(), "ADMIN", adminId);
        serviceRequestRepository.saveAndFlush(request);
        entityManager.refresh(request);
        return toResponse(request);
    }

    private ServiceRequestEntity getRequiredAccessibleRequest(
            Authentication authentication,
            Long id) {
        ServiceRequestEntity request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> notFound(
                        "Không tìm thấy Service Request."));
        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin()
                && !request.getCustomerId().equals(context.customerId())) {
            throw notFound("Không tìm thấy Service Request.");
        }
        return request;
    }

    private boolean isAllowedTransition(String currentStatus, String nextStatus) {
        return (ServiceRequestEntity.STATUS_NEW.equals(currentStatus)
                    && (ServiceRequestEntity.STATUS_IN_PROGRESS.equals(nextStatus)
                        || ServiceRequestEntity.STATUS_CANCELLED.equals(nextStatus)))
                || (ServiceRequestEntity.STATUS_IN_PROGRESS.equals(currentStatus)
                    && (ServiceRequestEntity.STATUS_DONE.equals(nextStatus)
                        || ServiceRequestEntity.STATUS_CANCELLED.equals(nextStatus)));
    }

    private LocalDateTime nowInApplicationZone() {
        LocalDateTime now = LocalDateTime.now(APPLICATION_ZONE);
        return now.withNano((now.getNano() / 1_000) * 1_000);
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    private ResponseStatusException notFound(String detail) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, detail);
    }

    private ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }

    private ServiceRequestResponse toResponse(ServiceRequestEntity request) {
        return new ServiceRequestResponse(
                request.getId(),
                request.getCustomerId(),
                request.getGameAccountId(),
                request.getServiceId(),
                request.getSnapshotServiceName(),
                request.getSnapshotPriceAmount(),
                request.getSnapshotCurrency(),
                request.getSnapshotPriceUnit(),
                request.getStatus(),
                request.getRequestedStartAt(),
                request.getStartAt(),
                request.getEndAt(),
                request.getDurationDays(),
                request.getNotes(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
