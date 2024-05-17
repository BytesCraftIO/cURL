package io.bytescraft.controller;

import io.bytescraft.cURL;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author javaquery
 * @since 2024-05-05
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping(produces = "application/json")
    @cURL(name = "Health Check")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok("");
    }
}
