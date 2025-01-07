package org.lite.product.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import jakarta.servlet.http.HttpServletRequest;
import org.lite.product.model.GreetingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${gateway.base-url}")
    private String gatewayBaseUrl; // Inject gateway base URL

    // Only work if the discovery client is Eureka
    private final EurekaClient eurekaClient;
    private final RestTemplate restTemplate;

    AtomicInteger requestCount = new AtomicInteger(1);

    // Wiring the Eureka Client
    @Autowired
    public ProductController(EurekaClient eurekaClient, RestTemplate restTemplate) {
        this.eurekaClient = eurekaClient;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/greet")
    public ResponseEntity<GreetingResponse> getProduct(HttpServletRequest request) {
        log.info("Greetings from Product Service!");
        
        try {
            com.netflix.discovery.shared.Application application = eurekaClient.getApplication(appName);
            if (application == null || application.getInstances().isEmpty()) {
                log.error("No instances found for application: {}", appName);
                return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
            }

            InstanceInfo service = application.getInstances().getFirst();
            GreetingResponse response = new GreetingResponse();
            response.setIndex(requestCount.getAndIncrement());
            response.setGreeting("Hello from Product service !!");
            response.setInstanceId(instanceId);
            response.setPort(service.getPort());
            response.setUrl(request.getRequestURL().toString());
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error getting service information: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Method to call Inventory Service from Product Service
    @GetMapping("/callInventory")
    public ResponseEntity<GreetingResponse> callInventoryService(){
        String url = gatewayBaseUrl + "/inventory/greet";
        try {
            GreetingResponse response = restTemplate.getForObject(url, GreetingResponse.class);
            log.info("Response from Inventory Service: {}", response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error calling Inventory Service", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
