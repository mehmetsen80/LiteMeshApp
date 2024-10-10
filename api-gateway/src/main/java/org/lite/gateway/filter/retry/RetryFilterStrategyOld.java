//package org.lite.gateway.filter;
//
//import io.github.resilience4j.retry.Retry;
//import io.github.resilience4j.retry.RetryConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.entity.ApiRoute;
//import org.lite.gateway.entity.FilterConfig;
//import org.lite.gateway.model.RetryRecord;
////import org.lite.gateway.service.SharedErrorContext;
//import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatusCode;
//
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import reactor.util.retry.RetryBackoffSpec;
//
//
//import java.net.URI;
//import java.time.Duration;
//
//
//@Slf4j
//public class RetryFilterStrategyOld implements FilterStrategy{
//
////    private final SharedErrorContext sharedErrorContext;
////
////    public RetryFilterStrategyOld(SharedErrorContext sharedErrorContext) {
////        this.sharedErrorContext = sharedErrorContext;
////    }
//
//    @Override
//    public void apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter) {
//        // Extract Retry parameters from FilterConfig
//        int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
//        Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
//        String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
//        String fallbackUri = filter.getArgs().get("fallbackUri");
//        String routeId = apiRoute.getRouteIdentifier();
//
//        RetryRecord retryRecord = new RetryRecord(routeId, maxAttempts, waitDuration, retryExceptions, fallbackUri);
//
//        log.info("Configuring Retry for route: {}, maxAttempts: {}, waitDuration: {}, retryExceptions: {}",
//                apiRoute.getRouteIdentifier(), maxAttempts, waitDuration, retryExceptions);
//
//
//        // Build RetryConfig based on parameters
//        RetryConfig.Builder<Object> retryConfigBuilder = RetryConfig.custom()
//                .retryOnException(throwable ->{
//                    if (retryExceptions != null && !retryExceptions.isEmpty()) {
//                        for (String exceptionClassName : retryExceptions.split(",")) {
//                            try {
//                                log.info("exceptionClassName: " + exceptionClassName.trim());
//                                Class<?> clazz = Class.forName(exceptionClassName.trim());
//                                if (clazz.isInstance(throwable)) {
//                                    return true; // Retry on this exception
//                                }
//                            } catch (ClassNotFoundException e) {
//                                log.warn("Invalid exception class: {}", exceptionClassName);
//                            }
//                        }
//                    }
//                    return false;// Don't retry if not in the list
//                })
//                .maxAttempts(maxAttempts)
//                .waitDuration(waitDuration);
//
//        // RetryConfig from dynamic route configuration
//        RetryConfig retryConfig = retryConfigBuilder.build();
//        Retry retry = Retry.of(apiRoute.getRouteIdentifier(), retryConfig);
//
//
//
////        gatewayFilterSpec.filter((exchange, chain) -> {
////            String routeId = apiRoute.getRouteIdentifier();
////            log.info("Applying Retry for route: {}", routeId);
////
////
////
////            // Proceed with the decorated response
////            return chain.filter(exchange.mutate().response(responseDecorator).build())
////                    .onErrorResume(throwable -> {
////                        // Handle upstream (pre-response) errors
////                        log.error("Upstream error encountered: {}", throwable.getMessage());
////                        exchange.getAttributes().put("originalError", throwable);
////                        return Mono.error(throwable); // Propagate original throwable
////                    })
////                    .then(
////                            Mono.defer(() -> {
////                                log.info("Inside Mono.defer()");
////                                // Check if there's a downstream 5xx error
////                                HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
////                                if (statusCode != null && statusCode.is5xxServerError()) {
////                                    log.error("5xx error for route: {}", routeId);
////                                    // Retrieve the original error if available
////                                    Throwable originalError = exchange.getAttribute("originalError");
////                                    if (originalError != null) {
////                                        return Mono.error(originalError); // Re-throw the original exception
////                                    } else {
////                                        return Mono.error(new RuntimeException("5xx Server Error")); // Create a new exception with the body
////                                    }
////                                }
////                                return Mono.empty(); // No error, just proceed
////                            })
////                            .onErrorResume(throwable -> {
////                                log.error("2nd Downstream error encountered: {}", throwable.getMessage());
////                                exchange.getAttributes().put("originalError", throwable);
////                                return Mono.error(throwable); // Propagate original throwable
////                            })
////                    .retryWhen(
////                            retryBackoffSpec
////                    )
////                    .onErrorResume(throwable -> {
////                        // Final fallback after retries are exhausted
////                        log.error("Retries failed for route: {}", routeId);
////
////                        // Retrieve and propagate the original downstream error if available
////                        Throwable originalError = exchange.getAttribute("originalError");
////                        if (originalError != null) {
////                            log.error("Propagating original error: {}", originalError.getMessage());
////                            return Mono.error(originalError); // Re-throw the original error after retries are exhausted
////                        }
////
////                        return Mono.error(throwable);
////                    }).then());
////
////        });
//
//
//
//        gatewayFilterSpec.filter((exchange, chain) -> {
//            String requestId = exchange.getRequest().getId(); // Same request ID used to store the error
//            log.info("Applying Retry for route: {}", routeId);
//            try {
//
////                // Cache the request body
////                AtomicReference<String> cachedRequestBodyObject = new AtomicReference<>();
////                ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
////                    @Override
////                    public @NonNull Flux<DataBuffer> getBody() {
////                        return super.getBody().doOnNext(dataBuffer -> {
////                            byte[] content = new byte[dataBuffer.readableByteCount()];
////                            dataBuffer.read(content);
////                            String bodyString = new String(content, StandardCharsets.UTF_8);
////                            cachedRequestBodyObject.set(bodyString); // Cache the request body
////                            log.info("Cached request body: {}", bodyString);
////                            DataBufferUtils.release(dataBuffer); // Release the buffer after reading
////                        });
////                    }
////                };
////
////                // Cache the response body
////                AtomicReference<String> cachedResponseBodyObject = new AtomicReference<>();
////                ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
////                    @Override
////                    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
////                        log.info("Inside response decorator writeWith");  // Log to check if we enter the decorator
////                        if (getStatusCode() != null && getStatusCode().is5xxServerError()) {
////                            log.info("5xx error detected, caching response body");  // Log for 5xx status detection
////                            return Flux.from(body)
////                                    .doOnNext(dataBuffer -> {
////                                        // Check if the response contains a body
////                                        byte[] content = new byte[dataBuffer.readableByteCount()];
////                                        dataBuffer.read(content);
////                                        String responseBody = new String(content, StandardCharsets.UTF_8);
////                                        cachedResponseBodyObject.set(responseBody); // Cache the response body
////                                        log.info("Captured response body: {}", responseBody);  // Log the response body
////
////                                        DataBufferUtils.release(dataBuffer); // Release the buffer after reading
////                                    })
////                                    .then(super.writeWith(body));  // Pass the data on after processing
////                        }
////                        return super.writeWith(body); // Pass through if no 5xx error
////                    }
////                };
//
//                // Wrap request and response using BodyCaptureExchange
////                BodyCaptureExchange bodyCaptureExchange = new BodyCaptureExchange(exchange);
//
//
//                RetryBackoffSpec retryBackoffSpec = reactor.util.retry.Retry.fixedDelay(retryConfig.getMaxAttempts(), waitDuration)
//                        .doBeforeRetry(retrySignal -> {
//                            long attempt = retrySignal.totalRetries() + 1;
//                            log.info("Retry attempt {} for route: {}", attempt, routeId);
//                        }).filter(throwable -> {
//                            log.info("throwable message: {}", throwable.getMessage());
//                            // Apply the same Resilience4j retryOnException logic to Reactor
//                            boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
//                            if (!shouldRetry) {
//                                log.info("Skipping retry for exception: {} on route: {}", throwable.getClass().getName(), routeId);
//                            }
//                            return shouldRetry;
//                        });
//
//
//                // Apply BodyCaptureFilter to capture request/response and store the original error
//                BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
//                BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
//
//                ServerWebExchange mutatedExchange = exchange.mutate()
//                        .request(requestDecorator)
//                        .response(responseDecorator)
//                        .build();
//
//                return chain.filter(mutatedExchange)
//                        .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
//                            // Handle upstream (pre-response) errors
//                            log.error("Upstream error encountered: {}", throwable.getMessage());
//                            exchange.getAttributes().put("originalError", throwable);
//                            return Mono.error(throwable);
//                         })
//                        .then(Mono.defer(() -> {
//
//                            log.info("2nd requestId: {}", requestId);
//
//                            // Check if there's a downstream 5xx error
//                            HttpStatusCode statusCode = responseDecorator.getStatusCode();
//                            log.info("Downstream response status: {}", statusCode);  // <== Log status code to see downstream response
//                            if (statusCode != null && statusCode.is5xxServerError()) {
//                                log.error("5xx error for route: {}", routeId);
//
//                                String error =  responseDecorator.getFullBody();
//                                log.info("error: {}", error);
//
////                                // Retrieve the cached response body if available
////                                String responseBody = bodyCaptureExchange.getResponse().getFullBody();
////                                log.info("Captured downstream response body: {}", responseBody);
//
//                                String originalErrorString = (String) exchange.getAttributes().get("originalError");
//                                if (originalErrorString != null) {
//                                    log.error("Original error string: {}", originalErrorString);
//                                    return Mono.error(new RuntimeException(originalErrorString)); // Rethrow the original exception
//                                }
////
////                                // Retrieve the original error stored in BodyCaptureFilter
////                                Throwable originalError = exchange.getAttribute("originalError");
////                                if (originalError != null) {
////                                    log.error("Original error: {}", originalError.getMessage());
////                                    return Mono.error(originalError); // Rethrow the original exception
////                                }
//
//                                // Retrieve the original error from Redis
////                                Throwable originalError = sharedErrorContext.retrieveError(requestId);
////                                log.info("originalError: {}", originalError.getMessage());
//                                //sharedErrorContext.removeError(requestId); // Optionally remove it after retrieving
//
//                                return Mono.error(new RuntimeException("5xx Server Error: " + error)); // Create a new exception with the error body
//                            }
//                            return Mono.empty();
//                        }))
//                        .retryWhen(retryBackoffSpec)
//                        .onErrorResume(throwable -> {
//                            // Final fallback after retries are exhausted
//                            log.error("Retries failed for route: {}", routeId);
//
////                            // Capture the root cause, to handle RetryExhaustedException appropriately
////                            if (throwable instanceof RetryExhaustedException) {
////                                log.error("Retries exhausted after maximum attempts for route: {}. Original exception: {}", routeId, throwable.getCause());
////                                // Handle retry exhausted case
////                                return Mono.error(throwable.getCause());  // Bubble up the original cause of failure
////                            }
////
////                            // Retrieve and propagate the original downstream error if available
////                            Throwable originalError = exchange.getAttribute("originalError");
////                            if (originalError != null) {
////                                log.error("Propagating original error: {}", originalError.getMessage());
////                                return Mono.error(originalError); // Re-throw the original error after retries are exhausted
////                            }
////
////                            // Otherwise, return the final throwable after retries
////                            return Mono.error(throwable);
//
//
//                            // Retrieve the captured request and response bodies
////                            String capturedRequestBody = requestDecorator.getFullBody();
////                            String capturedResponseBody = responseDecorator.getFullBody();
////
////                            log.info("Captured Request Body for retry failure: {}", capturedRequestBody);
////                            log.info("Captured Response Body for retry failure: {}", capturedResponseBody);
//
//                            // Retrieve the original error stored in BodyCaptureFilter
//                            Throwable originalError = exchange.getAttribute("originalError");
//                            if (originalError != null) {
//                                log.error("Original error on retry failure: {}", originalError.getMessage());
//                            }
//
//                            // Redirect to fallback URI with an appropriate response
//                            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
//                            exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUri));
//                            return exchange.getResponse().setComplete();
//                        }).then();
//
//
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////        //This works fine, just doesn't get the original exception message from the downstream
////        gatewayFilterSpec.filter((exchange, chain) -> {
////            String routeId = apiRoute.getRouteIdentifier();
////            log.info("Applying Retry for route: {}", routeId);
////
////            try {
////
////                // Cache the request body
////                AtomicReference<String> cachedRequestBodyObject = new AtomicReference<>();
////                ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
////                    @Override
////                    public Flux<DataBuffer> getBody() {
////                        return super.getBody().doOnNext(dataBuffer -> {
////                            byte[] content = new byte[dataBuffer.readableByteCount()];
////                            dataBuffer.read(content);
////                            String bodyString = new String(content, StandardCharsets.UTF_8);
////                            cachedRequestBodyObject.set(bodyString); // Cache the request body
////                            log.info("Cached request body: {}", bodyString);
////                            DataBufferUtils.release(dataBuffer); // Release the buffer after reading
////                        });
////                    }
////                };
////
////                // Cache the response body
////                AtomicReference<String> cachedResponseBodyObject = new AtomicReference<>();
////                ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
////                    @Override
////                    public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
////                        if (getStatusCode() != null && getStatusCode().is5xxServerError()) {
////                            return Flux.from(body)
////                                    .collectList() // Collect all DataBuffers into a list
////                                    .flatMap(dataBuffers -> {
////                                        // Join the DataBuffers into one buffer and extract content
////                                        DataBuffer joinedBuffer = exchange.getResponse().bufferFactory().join(dataBuffers);
////                                        byte[] content = new byte[joinedBuffer.readableByteCount()];
////                                        joinedBuffer.read(content);
////
////                                        // Convert the content to a string (downstream error message)
////                                        String responseBody = new String(content, StandardCharsets.UTF_8);
////                                        cachedResponseBodyObject.set(responseBody); // Cache the response body
////                                        log.error("Captured and cached 5xx response body: {}", responseBody);
////
////                                        // Release the DataBuffers after reading
////                                        DataBufferUtils.release(joinedBuffer);
////                                        dataBuffers.forEach(DataBufferUtils::release);
////
////                                        // Write the original content back to the response
////                                        return super.writeWith(Flux.just(exchange.getResponse().bufferFactory().wrap(content)));
////                                    });
////                        }
////                        return super.writeWith(body); // Pass through if no 5xx error
////                    }
////                };
////
////
////                RetryBackoffSpec retryBackoffSpec = reactor.util.retry.Retry.fixedDelay(retryConfig.getMaxAttempts(), waitDuration)
////                        .doBeforeRetry(retrySignal -> {
////                            long attempt = retrySignal.totalRetries() + 1;
////                            log.info("Retry attempt {} for route: {}", attempt, routeId);
////                        }).filter(throwable -> {
////                            log.info("throwable message: {}", throwable.getMessage());
////                            // Apply the same Resilience4j retryOnException logic to Reactor
////                            boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
////                            if (!shouldRetry) {
////                                log.info("Skipping retry for exception: {} on route: {}", throwable.getClass().getName(), routeId);
////                            }
////                            return shouldRetry;
////                        });
////
////                // Wrap the entire chain in the retryable block
////                return Retry.decorateCheckedSupplier(retry, () ->
////                                chain.filter(exchange.mutate().request(requestDecorator).response(responseDecorator).build())
////                                        .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
////                                            // Handle upstream (pre-response) errors
////                                            log.error("Upstream error encountered: {}", throwable.getMessage());
////                                            exchange.getAttributes().put("originalError", throwable);
////                                            return Mono.error(throwable);
////                                        }).then(
////                                                Mono.defer(() -> { // Process the response, catches the downstream (i.e inventory-service) exceptions
////                                                            // Check if there's a downstream 5xx error
////                                                            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
////                                                            if (statusCode != null && statusCode.is5xxServerError()) {
////                                                                log.error("5xx error for route: {}", routeId);
////
////                                                                // Retrieve the cached response body if available
////                                                                String errorResponseBody = cachedResponseBodyObject.get();
////                                                                log.error("Downstream error body: {}", errorResponseBody);
////
////                                                                // Retrieve the original error if available
////                                                                Throwable originalError = exchange.getAttribute("originalError");
////                                                                if (originalError != null) {
////                                                                    return Mono.error(originalError); // Re-throw the original exception
////                                                                } else {
////                                                                    return Mono.error(new RuntimeException("5xx Server Error: " + errorResponseBody)); // Create a new exception with the error body
////                                                                }
////                                                            }
////                                                            return Mono.empty();
////                                                }).onErrorResume(throwable -> {
////                                                    log.error("Inside 2nd onErrorResume");
////                                                    // Store the original error in exchange attributes
////                                                    exchange.getAttributes().put("originalError", throwable);
////                                                    // Log and propagate the original error
////                                                    log.error("TTT Error encountered for route: {}, Exception: {}", routeId, throwable.getMessage());
////
////                                                    // Log and propagate the original error (unwrap if necessary)
////                                                    Throwable rootCause = throwable.getCause() != null ? throwable.getCause() : throwable;
////                                                    log.error("Root cause: {}", rootCause.getMessage());
////                                                    return Mono.error(rootCause); // Handle any other exception
////
////                                                    //return Mono.error(throwable); // Propagate the original error
////                                                })
////                                        )
////                                        .retryWhen(retryBackoffSpec)
////
////                        ).get()
////                        .onErrorResume( throwable ->  {
////                            // Final fallback after retries are exhausted
////                            log.error("Retries failed for route: {}: {}", routeId, throwable.getMessage());
//////                            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
//////                            exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUri));
//////                            return exchange.getResponse().setComplete();
////
////                            // Retrieve and propagate the original downstream error if available
////                            Throwable originalError = exchange.getAttribute("originalError");
////                            if (originalError != null) {
////                                log.error("Propagating original error: {}", originalError.getMessage());
////                                return Mono.error(originalError); // Re-throw the original error after retries are exhausted
////                            }
////
////                            return Mono.error(throwable);
////                        })
////                        .then();
////
////            } catch (Throwable e) {
////                throw new RuntimeException(e);
////            }
////        });
//
//
//
//
//
//
//
//
//
////        //working example
////        gatewayFilterSpec.filter((exchange, chain) -> {
////            String routeId = apiRoute.getRouteIdentifier();
////            log.info("Applying Retry for route: {}", routeId);
////
////            try {
////
////                RetryBackoffSpec retryBackoffSpec = reactor.util.retry.Retry.fixedDelay(retryConfig.getMaxAttempts(), waitDuration)
////                        .doBeforeRetry(retrySignal -> {
////                            long attempt = retrySignal.totalRetries() + 1;
////                            log.info("Retry attempt {} for route: {}", attempt, routeId);
////                        }).filter(throwable -> {
////                            // Apply the same Resilience4j retryOnException logic to Reactor
////                            boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
////                            if (!shouldRetry) {
////                                log.info("Skipping retry for exception: {} on route: {}", throwable.getClass().getName(), routeId);
////                            }
////                            return shouldRetry;
////                        });
////
////                // Wrap the entire chain in the retryable block
////                return Retry.decorateCheckedSupplier(retry, () ->
////                                chain.filter(exchange)
////                                        .onErrorResume(throwable -> { // Handle network errors or any other throwable
////                                            log.error("Inside onErrorResume");
////                                            // Store the original error in exchange attributes
////                                            exchange.getAttributes().put("originalError", throwable);
////                                            // Log and propagate the original error for retry to catch
////                                            log.error("Error encountered for route: {}, Exception: {}", routeId, throwable.getMessage());
////                                            return Mono.error(throwable); // propagate the original error for retry to catch
////                                        }).then(Mono.defer(() -> {
////                                            // Check if the response has a 5xx status code
////                                            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
////                                            if (statusCode != null && statusCode.is5xxServerError()) {
////                                                log.error("5xx error for route: {}, triggering retry.", routeId);
////                                                // Propagate the original exception if it exists
////                                                Throwable originalError = exchange.getAttribute("originalError"); // Storing the original exception for reuse
////                                                if (originalError != null) {
////                                                    log.info("Original Error: {}", originalError.getMessage());
////                                                    return Mono.error(originalError); // rethrow the original exception
////                                                }
////                                                return Mono.error(new RuntimeException("5xx Server Error")); // fallback in case no original exception exists
////                                            }
////                                            return Mono.empty();
////                                        })
////                        )
////                        .retryWhen(
////                                retryBackoffSpec
////                        )
////                        .onErrorResume(throwable -> {
////                            // Final fallback after retries are exhausted
////                            log.error("Retries failed for route: {}, fallback to: {}", routeId, fallbackUri);
////                            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
////                            exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUri));
////                            return exchange.getResponse().setComplete();
////                        })).get().then();
////            } catch (Throwable e) {
////                throw new RuntimeException(e);
////            }
////        });
//    }
//}
