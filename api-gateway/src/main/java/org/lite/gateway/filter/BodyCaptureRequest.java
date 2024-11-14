package org.lite.gateway.filter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

@Slf4j
public class BodyCaptureRequest extends ServerHttpRequestDecorator {

    public BodyCaptureRequest(ServerHttpRequest delegate) {
        super(delegate);
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(super.getHeaders());

        log.info(super.getHeaders().toString());

        // Ensure that the Authorization header is copied
        if (super.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.info("Inside BodyCaptureRequest");
            headers.set(HttpHeaders.AUTHORIZATION, super.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        }

        log.info("Headers set for internal forwarding in BodyCaptureRequest");
        return headers;
    }

    @Override
    public @NonNull Flux<DataBuffer> getBody() {
        return super.getBody();
    }
}
