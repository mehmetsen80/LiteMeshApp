package org.lite.gateway.filter;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class BodyCaptureResponse extends  ServerHttpResponseDecorator {

    private final StringBuilder fullBody = new StringBuilder();
    private Mono<String> bodyCaptureMono;  // Cached Mono to capture the body later

    private final StringBuilder bodyBuffer = new StringBuilder();  // Buffer to store response body

    public BodyCaptureResponse(ServerHttpResponse delegate) {
        super(delegate);
    }

    // Capture the body as a Flux of DataBuffers and collect them into a String
    @Override
    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        Flux<? extends DataBuffer> buffer = Flux.from(body);
        // Capture the body data into the bodyBuffer
        return super.writeWith(buffer.map(dataBuffer -> {
            // Convert the DataBuffer to a String and append to the body buffer
            byte[] content = "".getBytes();
            if (dataBuffer.readableByteCount() > 0) {
                content = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(content);
                String bodyString = new String(content, StandardCharsets.UTF_8);
                log.info("bodyString: {}", bodyString);
                bodyBuffer.append(bodyString);  // Append to the StringBuilder
            } else {
                log.info("No readable data in dataBuffer");
            }
            DataBufferUtils.release(dataBuffer);  // Release the buffer after reading
            return bufferFactory().wrap(content);  // Return a wrapped buffer for further processing
        }));
    }


    @Override
    public @NonNull HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(super.getHeaders());

        // Preserve the original content type if it exists
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        
        // Copy Authorization header if it exists
        if (super.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            headers.set(HttpHeaders.AUTHORIZATION, super.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        }

        return headers;
    }

    // Return the captured full body as a String
    public String getFullBody() {
        return bodyBuffer.toString();
    }

    // Provide access to the body as a Mono or Flux (you can modify this as needed)
    public Mono<String> getBody() {
        // Return the body as a Mono
        return Mono.justOrEmpty(getFullBody());
    }
}
