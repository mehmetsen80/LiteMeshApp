package org.lite.product.model;


import lombok.Data;

@Data
public class GreetingResponse {
    private String greeting;
    private String instanceId;
    private int port;
    private String url;
}
