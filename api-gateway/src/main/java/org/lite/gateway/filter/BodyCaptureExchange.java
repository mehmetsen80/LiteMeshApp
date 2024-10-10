package org.lite.gateway.filter;

import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

//NOT NEEDED RIGHT NOW, DELETE LATER
public class BodyCaptureExchange extends ServerWebExchangeDecorator {

    private final BodyCaptureRequest bodyCaptureRequest;
    private final BodyCaptureResponse bodyCaptureResponse;

    public BodyCaptureExchange(ServerWebExchange exchange) {
        super(exchange);
        this.bodyCaptureRequest = new BodyCaptureRequest(exchange.getRequest());
        this.bodyCaptureResponse = new BodyCaptureResponse(exchange.getResponse());
    }

    @Override
    public @NonNull BodyCaptureRequest getRequest() {
        return bodyCaptureRequest;
    }

    @Override
    public @NonNull BodyCaptureResponse getResponse() {
        return bodyCaptureResponse;
    }

}
