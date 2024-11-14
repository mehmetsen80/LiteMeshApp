//package org.lite.gateway.config;
//
//import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLSession;
//import org.springframework.context.annotation.Configuration;
//import java.util.Set;
//
//@Configuration
//public class SSLConfig {
//
//    private static final Set<String> ALLOWED_HOSTNAMES = Set.of(
//            "localhost",
//            "127.0.0.1",
//            "api-gateway-service",
//            "inventory-service",
//            "product-service"
//    );
//
//    public SSLConfig() {
//        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
//            @Override
//            public boolean verify(String hostname, SSLSession sslSession) {
//                // Allow bypass for specified hostnames
//                return ALLOWED_HOSTNAMES.contains(hostname);
//            }
//        });
//    }
//}
