package com.puppyshome.backend.gameaccount;

import com.puppyshome.backend.auth.CustomerEntity;
import com.puppyshome.backend.auth.CustomerRepository;
import com.puppyshome.backend.gameaccount.dto.GameAccountResponse;
import com.puppyshome.backend.gameaccount.dto.GameAccountStatusRequest;
import com.puppyshome.backend.gameaccount.dto.GameAccountUpsertRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GameAccountService {

    private final GameAccountRepository gameAccountRepository;
    private final CustomerRepository customerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public GameAccountService(
            GameAccountRepository gameAccountRepository,
            CustomerRepository customerRepository) {
        this.gameAccountRepository = gameAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<GameAccountResponse> getGameAccounts(Authentication authentication) {
        if (isAdmin(authentication)) {
            return gameAccountRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return gameAccountRepository
                .findAllByCustomer_IdOrderByIdAsc(resolveCustomerId(authentication))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameAccountResponse getGameAccount(
            Authentication authentication,
            Long id) {
        if (isAdmin(authentication)) {
            return toResponse(getRequiredAccount(id));
        }

        return toResponse(getRequiredOwnedAccount(id, resolveCustomerId(authentication)));
    }

    @Transactional
    public GameAccountResponse createGameAccount(
            Authentication authentication,
            GameAccountUpsertRequest request) {
        CustomerEntity customer = getRequiredCustomer(authentication);

        GameAccountEntity account = new GameAccountEntity(
                customer,
                request.name(),
                Boolean.TRUE.equals(request.nocape()));

        gameAccountRepository.saveAndFlush(account);
        entityManager.refresh(account);

        return toResponse(account);
    }

    @Transactional
    public GameAccountResponse updateGameAccount(
            Authentication authentication,
            Long id,
            GameAccountUpsertRequest request) {
        GameAccountEntity account =
                getRequiredOwnedAccount(id, resolveCustomerId(authentication));

        account.updateDetails(
                request.name(),
                Boolean.TRUE.equals(request.nocape()));

        gameAccountRepository.saveAndFlush(account);
        entityManager.refresh(account);

        return toResponse(account);
    }

    @Transactional
    public void archiveGameAccount(Authentication authentication, Long id) {
        GameAccountEntity account =
                getRequiredOwnedAccount(id, resolveCustomerId(authentication));

        account.updateStatus(GameAccountEntity.STATUS_ARCHIVED);
        gameAccountRepository.saveAndFlush(account);
    }

    @Transactional
    public GameAccountResponse updateGameAccountStatus(
            Authentication authentication,
            Long id,
            GameAccountStatusRequest request) {
        GameAccountEntity account =
                getRequiredOwnedAccount(id, resolveCustomerId(authentication));

        account.updateStatus(request.status());
        gameAccountRepository.saveAndFlush(account);
        entityManager.refresh(account);

        return toResponse(account);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Long resolveCustomerId(Authentication authentication) {
        return getRequiredCustomer(authentication).getId();
    }

    private CustomerEntity getRequiredCustomer(Authentication authentication) {
        return customerRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Customer."));
    }

    private GameAccountEntity getRequiredAccount(Long id) {
        return gameAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Game Account."));
    }

    private GameAccountEntity getRequiredOwnedAccount(
            Long id,
            Long customerId) {
        GameAccountEntity account = getRequiredAccount(id);

        if (!account.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy Game Account.");
        }

        return account;
    }

    private GameAccountResponse toResponse(GameAccountEntity account) {
        return new GameAccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getName(),
                account.isNocape(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
