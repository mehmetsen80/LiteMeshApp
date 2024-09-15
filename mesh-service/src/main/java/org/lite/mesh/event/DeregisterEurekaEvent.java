package org.lite.mesh.event;

import org.lite.mesh.request.ServiceRegistrationRequest;
import org.springframework.context.ApplicationEvent;

public class DeregisterEurekaEvent extends ApplicationEvent {
    public DeregisterEurekaEvent(ServiceRegistrationRequest source) {
        super(source);
    }
}
