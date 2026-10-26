package com.puppyshome.backend.auth;

import java.util.List;

import com.puppyshome.backend.auth.dto.LoginRequest;
import com.puppyshome.backend.auth.dto.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (loginRequest.username() == null
                || loginRequest.username().isBlank()
                || loginRequest.password() == null
                || loginRequest.password().isEmpty()) {

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Username và password không được để trống."
            );

            return ResponseEntity.badRequest().body(problem);
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ."
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(problem);
        }

        // Đổi session ID nếu request hiện tại đã có session.
        // Việc này giúp hạn chế nguy cơ session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        List<String> authorities =
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        LoginResponse loginResponse = new LoginResponse(
                authentication.getName(),
                authorities
        );

        return ResponseEntity.ok(loginResponse);
    }
}