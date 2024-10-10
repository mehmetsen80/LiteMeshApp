package org.lite.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

//NOT USED RIGHT NOW BUT WE MIGHT NEED IT IN THE FUTURE
public class BodyCaptureRequest extends ServerHttpRequestDecorator {

    private final StringBuilder body = new StringBuilder();

    public BodyCaptureRequest(ServerHttpRequest delegate) {
        super(delegate);
    }

    public @NonNull Flux<DataBuffer> getBody() {
        return super.getBody().doOnNext(this::capture);
    }

    private void capture(DataBuffer buffer) {
        this.body.append(StandardCharsets.UTF_8.decode(buffer.toByteBuffer()));
    }

    public String getFullBody() {
        return this.body.toString();
    }
}
