package com.puppyshome.backend.auth;

import java.util.List;

import com.puppyshome.backend.auth.dto.LoginRequest;
import com.puppyshome.backend.auth.dto.LoginResponse;
import com.puppyshome.backend.auth.dto.RegisterRequest;
import com.puppyshome.backend.auth.CustomerEntity;
import com.puppyshome.backend.auth.CustomerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String email = registerRequest.email().trim().toLowerCase();

        if (customerRepository.existsByEmail(email)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Email đã được sử dụng.");
            problem.setTitle("EMAIL_EXISTS");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        CustomerEntity customer = new CustomerEntity(
                email,
                passwordEncoder.encode(registerRequest.password()));

        try {
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Email đã được sử dụng.");
            problem.setTitle("EMAIL_EXISTS");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            registerRequest.password()));
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Không thể tạo phiên sau khi đăng ký.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }

        saveAuthenticationSession(authentication, request, response);

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        LoginResponse loginResponse = new LoginResponse(
                authentication.getName(),
                authorities);

        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (loginRequest.username() == null
                || loginRequest.username().isBlank()
                || loginRequest.password() == null
                || loginRequest.password().isEmpty()) {

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Username và password không được để trống.");

            return ResponseEntity.badRequest().body(problem);
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.username(),
                            loginRequest.password()));
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(problem);
        }

        saveAuthenticationSession(authentication, request, response);

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        LoginResponse loginResponse = new LoginResponse(
                authentication.getName(),
                authorities);

        return ResponseEntity.ok(loginResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dữ liệu đăng ký không hợp lệ.");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail);
        problem.setTitle("VALIDATION_ERROR");

        return ResponseEntity.badRequest().body(problem);
    }

    private void saveAuthenticationSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(
                context,
                request,
                response);
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> currentAdmin(
            Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        LoginResponse response = new LoginResponse(
                authentication.getName(),
                authorities);

        return ResponseEntity.ok(response);
    }
}