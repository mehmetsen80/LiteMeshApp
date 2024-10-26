package org.lite.inventory.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.lite.inventory.model.GreetingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/inventory")
@Slf4j
public class InventoryController {

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    // Only work if the discovery client is Eureka
    private final EurekaClient eurekaClient;

    AtomicInteger requestCount = new AtomicInteger(1);

    // Wiring the Eureka Client
    public InventoryController(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @GetMapping("/greet")
    public ResponseEntity<GreetingResponse> getInventory(HttpServletRequest request) {
        log.info("Greetings from Inventory Service!");
        InstanceInfo service = eurekaClient.getApplication(appName).getInstances().get(0);
        GreetingResponse response = new GreetingResponse();
        response.setIndex(requestCount.getAndIncrement());
        response.setGreeting("Hello from Inventory service !!");
        response.setInstanceId(instanceId);
        response.setPort(service.getPort());
        response.setUrl(request.getRequestURL().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //to test the fallback
    @GetMapping("/testcircuitbreaker")
    public ResponseEntity<String> getSubItems() throws IOException, InterruptedException {
//        requestCount++;
//        if (requestCount < 5) {
//            throw new RuntimeException("Simulating failure on request #" + requestCount);
//        }
//        return ResponseEntity.ok("List of inventory items");


        //Thread.sleep(1000);
        throw new RuntimeException("Simulating failure on request");
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
        // Simulate delay (e.g., 10 seconds)
        Thread.sleep(20000);  // 10000 milliseconds = 10 seconds
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
