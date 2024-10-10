package org.lite.gateway.filter;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BodyCaptureResponse extends  ServerHttpResponseDecorator {

    private final StringBuilder fullBody = new StringBuilder();
    //private Mono<String> bodyCaptureMono;  // Cached Mono to capture the body later

    public BodyCaptureResponse(ServerHttpResponse delegate) {
        super(delegate);
    }

//    @Override
//    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
//        Flux<? extends DataBuffer> buffer = Flux.from(body);
//
//        log.info("inside writeWith");
//        // Cache the body so it can be read and written multiple times
//        Flux<DataBuffer> cachedBody = buffer.cache().cast(DataBuffer.class);
//
//        // Capture the body content and store it in bodyCaptureMono
//        this.bodyCaptureMono = DataBufferUtils.join(cachedBody)
//                .map(dataBuffer -> {
//                    byte[] content = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(content);
//                    DataBufferUtils.release(dataBuffer);  // Release the buffer
//
//                    String bodyContent = new String(content, StandardCharsets.UTF_8);
//                    this.fullBody.append(bodyContent);  // Append the body content
//
//                    log.info("Captured response body: {}", bodyContent);
//                    return bodyContent;  // Return the body content
//                }).defaultIfEmpty("");  // Ensure it never returns empty
//
//        // Write the cached body back to the response for downstream processing
//        getDelegate().setComplete();
//        return super.writeWith(cachedBody);
//    }
//
//    public Mono<String> getBody() {
//        return this.bodyCaptureMono != null ? this.bodyCaptureMono : Mono.just("");
//    }



    @Override
    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        Flux<? extends DataBuffer> buffer = Flux.from(body);
        log.info("buffer: {}", buffer);

        // Collect the body
        return DataBufferUtils.join(buffer)
                .flatMap(dataBuffer -> {

                    log.info("inside writeWith flatMap");

                    // Capture the body as a string
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);  // Release the buffer

                    String bodyContent = new String(content, StandardCharsets.UTF_8);
                    this.fullBody.append(bodyContent);  // Store the body content

                    log.info("fullBody: {}", fullBody);

                    // Wrap the body again for downstream processing
                    DataBuffer newBuffer = bufferFactory().wrap(bodyContent.getBytes(StandardCharsets.UTF_8));
                    return super.writeWith(Flux.just(newBuffer));
                });
    }

    public String getFullBody() {
        log.info("inside getFullBody()");
        log.info(this.fullBody.toString());
        return this.fullBody.toString();
    }
}
