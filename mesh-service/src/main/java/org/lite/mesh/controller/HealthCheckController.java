package org.lite.mesh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mesh")
public class HealthCheckController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping(){
        return ResponseEntity.ok("Application is healthy!");
    }
}
