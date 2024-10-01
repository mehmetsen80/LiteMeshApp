package org.lite.inventory.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import jakarta.servlet.http.HttpServletRequest;
import org.lite.inventory.model.GreetingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<GreetingResponse> getInventory(HttpServletRequest request) {
        log.info("Greetings from Inventory Service!");
        InstanceInfo service = eurekaClient.getApplication(appName).getInstances().get(0);
        GreetingResponse response = new GreetingResponse();
        response.setGreeting("Hello from Inventory service !!");
        response.setInstanceId(instanceId);
        response.setPort(service.getPort());
        response.setUrl(request.getRequestURL().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //to test the fallback
    @GetMapping("/items")
    public ResponseEntity<String> getItems() throws InterruptedException {
        // Simulate delay (e.g., 5 seconds)
        Thread.sleep(20000);  // 5000 milliseconds = 5 seconds
        return ResponseEntity.ok("Simulated slow response from inventory-service");
    }

    //to test the fallback
    @GetMapping("/subitems")
    public ResponseEntity<String> getSubItems() {
        //return ResponseEntity.ok("Inventory subitems");
        //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Simulated failure from inventory-service");
        // Simulate a failure
        throw new RuntimeException("Simulated inventory service failure");
    }
}
