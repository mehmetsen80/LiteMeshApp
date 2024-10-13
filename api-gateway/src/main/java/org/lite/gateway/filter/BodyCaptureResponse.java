package org.lite.gateway.filter;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
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

        log.info("inside new writeWith");

        Flux<? extends DataBuffer> buffer = Flux.from(body);

        // Capture the body data into the bodyBuffer
        return super.writeWith(buffer.map(dataBuffer -> {
            // Convert the DataBuffer to a String and append to the body buffer
            byte[] content = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(content);
            String bodyString = new String(content, StandardCharsets.UTF_8);

            log.info("bodyString: {}", bodyString);

            bodyBuffer.append(bodyString);  // Append to the StringBuilder
            DataBufferUtils.release(dataBuffer);  // Release the buffer after reading
            return bufferFactory().wrap(content);  // Return a wrapped buffer for further processing
        }));
    }


    @Override
    public @NonNull HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(super.getHeaders());

        // Ensure that the Authorization header is copied
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





//    @Override
//    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> bodyPublisher) {
//        // Capture the body content by reading the DataBuffer
//        Flux<DataBuffer> dataBufferFlux = Flux.from(bodyPublisher)
//                .map(dataBuffer -> {
//                    // Convert DataBuffer to String and append to body
//                    byte[] content = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(content);
//                    String bodyContent = new String(content, StandardCharsets.UTF_8);
//                    fullBody.append(bodyContent);
//
//                    // Retain the buffer for further use and release when done
//                    DataBufferUtils.retain(dataBuffer);
//                    return dataBuffer;
//                });
//
//        // Write the buffer back to the actual response
//        return super.writeWith(dataBufferFlux);
//    }
//








    //this works actually
//    @Override
//    public @NonNull Mono<Void> writeAndFlushWith(@NonNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
//        return super.writeAndFlushWith(body);
//    }
//
//
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
////
//    public Mono<String> getBody() {
//        if(this.bodyCaptureMono != null){
//            log.info("bodyCaptureMono is not null:");
//            log.info(String.valueOf(Mono.just(this.bodyCaptureMono)));
//            return this.bodyCaptureMono;
//        } else {
//            log.info("bodyCaptureMono is null!!!");
//            return Mono.just("");
//        }
//        //return this.bodyCaptureMono != null ? this.bodyCaptureMono : Mono.just("");
//    }











//    @Override
//    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
//        Flux<? extends DataBuffer> buffer = Flux.from(body);
//        log.info("buffer: {}", buffer);
//
//        // Collect the body
//        return DataBufferUtils.join(buffer)
//                .flatMap(dataBuffer -> {
//
//                    log.info("inside writeWith flatMap");
//
//                    // Capture the body as a string
//                    byte[] content = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(content);
//                    DataBufferUtils.release(dataBuffer);  // Release the buffer
//
//                    String bodyContent = new String(content, StandardCharsets.UTF_8);
//                    this.fullBody.append(bodyContent);  // Store the body content
//
//                    log.info("fullBody: {}", fullBody);
//
//                    // Wrap the body again for downstream processing
//                    DataBuffer newBuffer = bufferFactory().wrap(bodyContent.getBytes(StandardCharsets.UTF_8));
//                    return super.writeWith(Flux.just(newBuffer));
//                });
//    }

//    public String getFullBody() {
//        log.info("inside getFullBody()");
//        log.info(this.fullBody.toString());
//        return this.fullBody.toString();
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE;
//    }
}
