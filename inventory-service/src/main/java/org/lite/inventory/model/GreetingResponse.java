package org.lite.inventory.model;


import lombok.Data;

@Data
public class GreetingResponse {
    private int index;
    private String greeting;
    private String instanceId;
    private int port;
    private String url;
}
