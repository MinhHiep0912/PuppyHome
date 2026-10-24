package com.puppyshome.backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.puppyshome.backend.health.HealthResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse getHealth() {
        return new HealthResponse("UP", "puppys-home");
    }
}