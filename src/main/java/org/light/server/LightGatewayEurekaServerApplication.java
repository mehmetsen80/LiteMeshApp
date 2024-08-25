package org.light.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class LightGatewayEurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LightGatewayEurekaServerApplication.class, args);
    }
}
