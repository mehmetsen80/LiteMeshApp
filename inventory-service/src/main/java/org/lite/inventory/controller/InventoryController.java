package org.lite.inventory.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import org.lite.inventory.exception.InventoryServiceException;
import org.lite.inventory.model.GreetingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    // Only work if the discovery client is Eureka
    private final EurekaClient eurekaClient;

    // Wiring the Eureka Client
    public InventoryController(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @GetMapping("/greet")
    public ResponseEntity<GreetingResponse> getInventory(HttpServletRequest request, @RequestParam(required = false) String triggerError) throws InterruptedException {
        log.info("Greetings from Inventory Service!");
        InstanceInfo service = eurekaClient.getApplication(appName).getInstances().get(0);

        if ("true".equals(triggerError)) {
            TimeUnit.SECONDS.sleep(12);  // Wait for 10 seconds before responding
            // Simulate a server error (e.g., 500 Internal Server Error)
            throw new RuntimeException("Simulated server greet error");
        }

        GreetingResponse response = new GreetingResponse();
        response.setGreeting("Hello from Inventory service !!");
        response.setInstanceId(instanceId);
        response.setPort(service.getPort());
        response.setUrl(request.getRequestURL().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //to test the fallback
    @GetMapping("/testcircuitbreaker")
    public ResponseEntity<String> getSubItems() {
        //return ResponseEntity.ok("Inventory subitems");
        //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Simulated failure from inventory-service");
        // Simulate a failure
        throw new RuntimeException("Simulated inventory service failure falls to circuit breaker");
    }

    //to test the fallback
    @GetMapping("/testratelimiter")
    public ResponseEntity<String> testRateLimiter() throws InterruptedException {
        //TODO: change the below
        // Simulate delay (e.g., 5 seconds)
        Thread.sleep(10000);  // 10000 milliseconds = 5 seconds
        return ResponseEntity.ok("Simulated slow response from inventory-service");
    }

    @GetMapping("/testtimelimiter")
    public ResponseEntity<String> testTimeLimiter() throws InterruptedException {
        // Simulate delay (e.g., 5 seconds)
        Thread.sleep(10000);  // 10000 milliseconds = 5 seconds
        return ResponseEntity.ok("Simulated slow response from inventory-service");
    }


    @GetMapping("/testretry")
    //@Retry(name = "inventory-service")
    public ResponseEntity<String> testRetry() throws IOException {
        //throw new RuntimeException(new IOException("Simulated network error"));//always throw RuntimeException to the upstream

        throw new IOException("Simulated IOException network error"); // Simulated exception

        //throw new InventoryServiceException("Simulated network IOException error"); // Simulated exception
//        try {
//            // Your business logic
//            throw new IOException("Simulated network error"); // Simulated exception
//        } catch (IOException ex) {
//            log.error("IOException occurred: {}", ex.getMessage());
//            // Propagate the exception as a 500 (or other appropriate status) with the exception message
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Custom error message: " + ex.getMessage());
//        }
    }
}
