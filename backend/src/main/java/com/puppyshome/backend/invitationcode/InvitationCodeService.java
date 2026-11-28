package com.puppyshome.backend.invitationcode;

import com.puppyshome.backend.auth.AccessContext;
import com.puppyshome.backend.auth.AccessContextResolver;
import com.puppyshome.backend.employee.EmployeeProfileEntity;
import com.puppyshome.backend.employee.EmployeeProfileRepository;
import com.puppyshome.backend.gameaccount.GameAccountEntity;
import com.puppyshome.backend.gameaccount.GameAccountRepository;
import com.puppyshome.backend.invitationcode.dto.InvitationCodeResponse;
import com.puppyshome.backend.invitationcode.dto.InvitationCodeUpsertRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvitationCodeService {

    private final InvitationCodeRepository invitationCodeRepository;
    private final GameAccountRepository gameAccountRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final AccessContextResolver accessContextResolver;

    @PersistenceContext
    private EntityManager entityManager;

    public InvitationCodeService(
            InvitationCodeRepository invitationCodeRepository,
            GameAccountRepository gameAccountRepository,
            EmployeeProfileRepository employeeProfileRepository,
            AccessContextResolver accessContextResolver) {
        this.invitationCodeRepository = invitationCodeRepository;
        this.gameAccountRepository = gameAccountRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.accessContextResolver = accessContextResolver;
    }

    @Transactional(readOnly = true)
    public List<InvitationCodeResponse> getInvitationCodes(
            Authentication authentication,
            Long accountId) {
        GameAccountEntity account = getRequiredAccessibleAccount(authentication, accountId);
        return invitationCodeRepository
                .findAllByGameAccount_IdAndDeletedAtIsNullOrderByIdAsc(account.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvitationCodeResponse createInvitationCode(
            Authentication authentication,
            Long accountId,
            InvitationCodeUpsertRequest request) {
        GameAccountEntity account = getRequiredAccessibleAccount(authentication, accountId);
        if (!GameAccountEntity.STATUS_ACTIVE.equals(account.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_NOT_ACTIVE");
        }

        InvitationCodeEntity code = new InvitationCodeEntity(
                account,
                request.code().trim(),
                normalizeNote(request.note()));
        invitationCodeRepository.saveAndFlush(code);
        entityManager.refresh(code);
        return toResponse(code);
    }

    @Transactional
    public InvitationCodeResponse updateInvitationCode(
            Authentication authentication,
            Long codeId,
            InvitationCodeUpsertRequest request) {
        InvitationCodeEntity code = getRequiredAccessibleCode(authentication, codeId);
        code.update(request.code().trim(), normalizeNote(request.note()));
        invitationCodeRepository.saveAndFlush(code);
        entityManager.refresh(code);
        return toResponse(code);
    }

    @Transactional
    public void deleteInvitationCode(Authentication authentication, Long codeId) {
        InvitationCodeEntity code = invitationCodeRepository.findById(codeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Invitation Code."));
        ensureAccessible(authentication, code.getGameAccount());
        if (code.getDeletedAt() != null) {
            return;
        }
        code.softDelete(LocalDateTime.now());
        invitationCodeRepository.saveAndFlush(code);
    }

    @Transactional
    public InvitationCodeResponse markUsed(
            Authentication authentication,
            Long codeId,
            Long employeeId) {
        InvitationCodeEntity code = invitationCodeRepository.findByIdAndDeletedAtIsNull(codeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Invitation Code."));

        if (code.getUsedByEmployee() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CODE_ALREADY_USED");
        }

        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin() && !context.userId().equals(employeeId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "EMPLOYEE_MISMATCH");
        }

        EmployeeProfileEntity employee = employeeProfileRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Employee."));
        code.markUsed(employee, LocalDateTime.now());
        invitationCodeRepository.saveAndFlush(code);
        entityManager.refresh(code);
        return toResponse(code);
    }

    private GameAccountEntity getRequiredAccessibleAccount(
            Authentication authentication,
            Long accountId) {
        GameAccountEntity account = gameAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Game Account."));
        ensureAccessible(authentication, account);
        return account;
    }

    private InvitationCodeEntity getRequiredAccessibleCode(
            Authentication authentication,
            Long codeId) {
        InvitationCodeEntity code = invitationCodeRepository.findByIdAndDeletedAtIsNull(codeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Invitation Code."));
        ensureAccessible(authentication, code.getGameAccount());
        return code;
    }

    private void ensureAccessible(Authentication authentication, GameAccountEntity account) {
        AccessContext context = accessContextResolver.resolve(authentication);
        if (!context.admin()
                && !account.getCustomerId().equals(context.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy Game Account.");
        }
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private InvitationCodeResponse toResponse(InvitationCodeEntity code) {
        EmployeeProfileEntity employee = code.getUsedByEmployee();
        return new InvitationCodeResponse(
                code.getId(),
                code.getGameAccount().getId(),
                code.getCode(),
                code.getNote(),
                employee == null ? null : employee.getId(),
                employee == null ? null : employee.getDisplayName(),
                code.getUsedAt(),
                code.getCreatedAt(),
                code.getUpdatedAt());
    }
}
