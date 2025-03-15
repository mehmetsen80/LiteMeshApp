package org.lite.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import java.time.Duration;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

        ExchangeStrategies strategies = ExchangeStrategies
            .builder()
            .codecs(clientCodecConfigurer -> {
                clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(
                    new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON)
                );
                clientCodecConfigurer.defaultCodecs().jackson2JsonEncoder(
                    new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON)
                );
                clientCodecConfigurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
            })
            .build();

        ConnectionProvider provider = ConnectionProvider
            .builder("fixed")
            .maxConnections(500)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .evictInBackground(Duration.ofSeconds(30))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(5))
                .addHandlerLast(new WriteTimeoutHandler(5)))
            .secure()
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true);

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
