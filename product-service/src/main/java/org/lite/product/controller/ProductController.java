package org.lite.product.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import jakarta.servlet.http.HttpServletRequest;
import org.lite.product.model.GreetingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String appName;

    // Only work if the discovery client is Eureka
    private final EurekaClient eurekaClient;

    // Wiring the Eureka Client
    public ProductController(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @GetMapping("/greet")
    public ResponseEntity<GreetingResponse> getProduct(HttpServletRequest request) {
        log.info("Greetings from Product Service!");
        InstanceInfo service = eurekaClient.getApplication(appName).getInstances().get(0);
        GreetingResponse response = new GreetingResponse();
        response.setGreeting("Hello from Product service !!");
        response.setInstanceId(instanceId);
        response.setPort(service.getPort());
        response.setUrl(request.getRequestURL().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
