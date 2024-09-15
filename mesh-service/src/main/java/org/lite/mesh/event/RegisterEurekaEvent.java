package org.lite.mesh.event;

import org.lite.mesh.request.ServiceRegistrationRequest;
import org.springframework.context.ApplicationEvent;

public class RegisterEurekaEvent extends ApplicationEvent {
    public RegisterEurekaEvent(ServiceRegistrationRequest source) {
        super(source);
    }
}
