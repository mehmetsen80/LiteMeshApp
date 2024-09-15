package org.lite.mesh.request;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration(
        proxyBeanMethods = false
)
@ConfigurationProperties(prefix = "lite-mesh")
public class ServiceRegistrationRequest {

    private Application application;
    private Discovery discovery;

    public ServiceRegistrationRequest(){
        application = new Application();
        discovery = new Discovery();
    }

    @Data
    static public class Application{
        private String id;
        private String username;
    }

    @Data
    static public class Discovery{
        private String url;
        private int port;
        private String preferred;//eureka,none
    }

    private String registerUrl;
    private String deregisterUrl;

    public String getRegisterUrl() {
        return discovery.url + ":"  + discovery.port + "/api/register";
    }

    public String getDeregisterUrl() {
        return discovery.url + ":"  + discovery.port + "/api/deregister";
    }
}
