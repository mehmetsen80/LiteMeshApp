//package org.lite.gateway.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@RestController
//@Slf4j
//public class MessagesController {
//
//    private final WebClient webClient;
//
//    public MessagesController(WebClient webClient) {
//        this.webClient = webClient;
//    }
//
//    @GetMapping("/refresh")
//    public Mono<String> messages() {
//        return webClient.get()
//                .uri("http://localhost:7777/routes/refresh/routes")  // Dynamic service URI
//                .retrieve()
//                .bodyToMono(String.class)
//                .doOnError(error -> log.error("Error calling Refresh Service: ", error));
//    }
//
//}
