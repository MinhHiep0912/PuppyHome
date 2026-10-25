
package com.puppyshome.backend.service;

import com.puppyshome.backend.service.dto.PublicServiceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceAppService serviceAppService;

    public ServiceController(ServiceAppService serviceAppService) {
        this.serviceAppService = serviceAppService;
    }

    @GetMapping
    public ResponseEntity<List<PublicServiceResponse>> getPublishedServices() {
        List<PublicServiceResponse> services =
                serviceAppService.getPublishedServices();

        return ResponseEntity.ok(services);
    }
}
