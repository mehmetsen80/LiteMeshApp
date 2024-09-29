package org.lite.gateway.config;

import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableDiscoveryClient
//@ConditionalOnProperty(name = "spring.security.enabled", matchIfMissing = true)
public class EurekaClientConfig {

//    @Bean
//    public EurekaClient eurekaClient(EurekaClientConfigBean eurekaClientConfigBean) {
//        return new com.netflix.discovery.DiscoveryClient(eurekaClientConfigBean);
//    }
}
