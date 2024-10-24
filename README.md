# What is LiteMesh?
LiteMesh is designed as a lightweight, highly adaptable API gateway that addresses the challenges of dynamic routing, 
security, and resilience with simplicity and speed.

While other competitors offer complex and heavyweight solutions, 
LiteMesh provides a lean, developer-friendly platform that can grow to meet the demands of modern microservices and 
serverless architectures—positioning itself to become a leading solution as the API ecosystem evolves.

![Build Status](https://github.com/mehmetsen80/LiteMeshApp/actions/workflows/maven.yml/badge.svg) ![Java 21](https://img.shields.io/badge/Java-21-blue)

Go to the root folder where the root pom.xml resides and run the below to create all jar files;
```shell
cd ~/IdeaProjects/LiteMeshApp/
mvn clean package
```
This will create the following jar files

| Application       |       Jar File       |     Type     | Essential |
|-------------------|:--------------------:|:------------:|:---------:|
| api-gateway       |   LiteGateway.jar    |   Gateway    |    Yes    |
| discovery-server  | DiscoveryService.jar |  Discovery   |    Yes    |
| inventory-service | InventoryService.jar | Microservice | Optional  |
| mesh-service      |   MeshService.jar    |    Web UI    | Optional  |
| product-service   |  ProductService.jar  | Microservice | Optional  |


The api-gateway acts as the entry point for handling requests from the inventory-service and product-service. These services are simple microservices designed to demonstrate the gateway’s connectivity and functionality.

The mesh-service serves as the user interface, but it's optional to run—whether you include it or not is up to you.


## PREREQUISITES


## HOW TO RUN

You don't need to run all the services to validate the system's functionality. To test the core functionality, follow this order when starting the applications:
Please do not forget that we run even the localhost on https:

1. Start discovery-server
```shell
   cd ~/IdeaProjects/LiteMeshApp/discovery-server/target
   java -jar DiscoveryService.jar
```
2. Start api-gateway
```shell
   cd ~/IdeaProjects/LiteMeshApp/api-gateway/target
   java -Djavax.net.ssl.trustStore=../src/main/resources/gateway-truststore.jks  -Djavax.net.ssl.trustStorePassword=123456   -jar LiteGateway.jar
```
3. Start inventory-service
```shell
   cd ~/IdeaProjects/LiteMeshApp/inventory-service/target
   java -Djavax.net.ssl.trustStore=../src/main/resources/client-truststore.jks  -Djavax.net.ssl.trustStorePassword=123456   -jar InventoryService.jar
```


## HOW IT WORKS
LiteMesh operates at the center of the API ecosystem, serving as the primary gateway for all microservices communication. 
Instead of allowing direct interactions between services, LiteMesh channels all requests through the gateway, creating 
a streamlined workflow that reduces potential conflicts and enhances security. 

LiteMesh serves as the core of the entire API architecture, acting as the dynamic gateway for all microservices communication. 
It ensures that microservices interact through the gateway, preventing direct service-to-service communication and 
improving security and manageability. 

Dynamic routing allows LiteMesh to route requests efficiently based on service discovery, load balancing, and 
application-specific rules. Both the gateway and microservices register themselves with the Service Discovery component, 
enabling seamless discovery and routing based on service names, while also supporting load balancing across instances.

Security is enforced on two fronts: Client-to-Gateway security, using JWT validation with OAuth2 through an identity 
provider (e.g., Keycloak), and Gateway-to-Service security through mTLS (Mutual TLS), ensuring a secure and encrypted 
channel between services.

LiteMesh also supports comprehensive resiliency mechanisms, including:

- RedisRateLimiter
    ``
    which uses Redis to manage temporary rate-limiting data, even in distributed systems, controlling traffic and protecting services from overload.
    ``

- TimeLimiter,
    ``
    which prevents long-running requests by enforcing a timeout for each API call.
    ``

- CircuitBreaker
    ``
    which opens after detecting a failure threshold, preventing repeated failures from overwhelming services.
    ``

- Retry
    ``
    which retries failed calls a set number of times before giving up, improving reliability.
    ``

All these configurations—routing, security, and resiliency—are dynamically managed and stored in MongoDB, allowing 
LiteMesh to adapt in real-time and protect APIs from attacks, overload, and vulnerabilities.

See below for the high level of LiteMesh architecture:

<div align="center">
<a href="assets/LiteMesh.jpg"> <img class="w-100" alt="LiteMesh" src="assets/LiteMesh.jpg"></a>
</div>

## HOW CONFIGURATION DATA LOOK LIKE IN MONGODB?

As previously mentioned, the configuration is stored in MongoDB, allowing LiteMesh to load them dynamically. 
Below is an example snippet for the inventory-service and product-service microservices, demonstrating how routing 
and resilience configurations are stored and can be updated dynamically.

```
{
  "_id": "1",
  "routeIdentifier": "inventory-service",
  "uri": "lb://inventory-service",
  "method": "",
  "path": "/inventory/**",
  "filters": [
    {
      "name": "RedisRateLimiter",
      "args": {
        "replenishRate": "10",
        "burstCapacity": "20",
        "requestedTokens": "1"
      }
    },
    {
      "name": "TimeLimiter",
      "args": {
        "timeoutDuration": "30",
        "cancelRunningFuture": "true"
      }
    },
    {
      "name": "CircuitBreaker",
      "args": {
        "name": "inventoryCircuitBreaker",
        "fallbackUri": "/fallback/inventory",
        "slidingWindowSize": "2",
        "failureRateThreshold": "10",
        "waitDurationInOpenState": "PT10S",
        "permittedNumberOfCallsInHalfOpenState": "1",
        "recordFailurePredicate": "HttpResponsePredicate",
        "automaticTransitionFromOpenToHalfOpenEnabled": "true"
      }
    },
    {
      "name": "Retry",
      "args": {
        "maxAttempts": "3",
        "waitDuration": "PT2S",
        "retryExceptions": "java.io.IOException, java.net.SocketTimeoutException, java.lang.RuntimeException"
      }
    }
  ],
  "_class": "org.lite.gateway.entity.ApiRoute"
}

{
  "_id": "2",
  "routeIdentifier": "product-service",
  "uri": "lb://product-service",
  "method": "",
  "path": "/product/**",
  "filters": [
    {
      "name": "RedisRateLimiter",
      "args": {
        "replenishRate": "10",
        "burstCapacity": "20",
        "requestedTokens": "1"
      }
    },
    {
      "name": "TimeLimiter",
      "args": {
        "timeoutDuration": "30",
        "cancelRunningFuture": "true"
      }
    },
    {
      "name": "CircuitBreaker",
      "args": {
        "name": "productCircuitBreaker",
        "fallbackUri": "/fallback/product",
        "slidingWindowSize": "2",
        "failureRateThreshold": "10",
        "waitDurationInOpenState": "PT210S",
        "permittedNumberOfCallsInHalfOpenState": "3",
        "recordFailurePredicate": "HttpResponsePredicate",
        "automaticTransitionFromOpenToHalfOpenEnabled": "true"
      }
    },
    {
      "name": "Retry",
      "args": {
        "maxAttempts": "3",
        "waitDuration": "PT2S",
        "retryExceptions": "java.io.IOException, java.net.SocketTimeoutException, java.lang.RuntimeException"
      }
    }
  ],
  "_class": "org.lite.gateway.entity.ApiRoute"
}

```


## DYNAMIC ROUTING
We chose dynamic routing because of its ability to handle the challenges of modern API management systems, especially 
in environments where APIs are deployed across different regions, dynamically scaling, or continuously evolving. 

It ensures high availability, low latency, and real-time adaptation, which are key for businesses seeking agility in 
their API ecosystems. With dynamic routing, LiteMesh can easily support cloud-native architectures, microservices, and 
multi-region deployments, providing our users with a robust, future-proof solution. 

Dynamic routing offers several benefits, especially in modern distributed systems, microservices architectures, and API gateways like LiteMesh. Here’s why it’s crucial and how it works:

### Benefits of Dynamic Routing:

1. **Flexibility and Adaptability:**
    `` 
    Dynamic routing allows the system to automatically adjust routes based on changing network conditions, traffic loads, or server availability. This ensures optimal performance and minimal downtime, even as services evolve. 
    ``


2. **Real-Time Traffic Management:**
   ``
   With dynamic routing, traffic can be rerouted in real-time based on load, server health, or geographic proximity. This leads to better resource utilization, reduced latency, and improved user experiences.
   ``


3. **Scalability**
   ``
   As services scale up or down, dynamic routing can accommodate new instances or remove dead ones without requiring manual intervention. This is especially valuable in cloud environments where servers are added or removed dynamically.
   ``

4. **Centralized API Management:**
   ``
   Dynamic routing allows you to manage and adjust routing rules centrally without making changes to individual services or requiring them to restart. This simplifies the management of complex microservice architectures.
   ``


5. **Reduced Configuration Overhead:**
   ``
   With static routing, you need to manually update configurations when services or endpoints change. Dynamic routing eliminates this overhead by automatically adjusting to service changes.
   ``

<div align="center">
<a href="assets/DynamicRouting.jpg"> <img alt="LiteMesh Dynamic Routing" src="assets/DynamicRouting.jpg"></a>
</div>


## SECURITY
<p>In an API gateway like LiteMesh, security is critical for safeguarding sensitive data, preventing unauthorized access, 
and ensuring compliance with privacy and security regulations. </p>

<p>The gateway serves as a central point for managing and securing communication between clients and backend services, 
making it the front-line for security enforcement. By securing both external access (Client-to-Gateway) and internal 
communication (Gateway-to-Service), LiteMesh ensures that the entire data flow is protected from potential breaches or 
unauthorized access.</p>

<h3>Overall Benefits of This Dual-Layer Security Model</h3>

### Separation of Concerns:
- This dual-layer approach simplifies the architecture by keeping authentication/authorization concerns separate from 
service communication, allowing each layer to focus on its specific security functions.

### Zero-Trust Architecture:
- LiteMesh's approach to security embodies a zero-trust model, where no entity (client or service) is trusted by default, 
and every interaction must be authenticated and authorized.

### Flexible and Scalable Security:
- Using OAuth2 and JWT allows LiteMesh to handle millions of API requests, while mTLS ensures that all internal 
communications are locked down tightly. This makes the platform secure at scale.

### Reduced Attack Surface:
- By validating tokens at the gateway and enforcing mTLS for internal communication, LiteMesh reduces the attack 
surface, making it harder for attackers to breach or compromise any part of the system.


In summary, the client-to-gateway security ensures that only trusted clients with valid credentials can access the APIs,
while the gateway-to-service security guarantees secure and trusted communication within the system.
This holistic approach creates a robust, secure API management platform, protecting both external and internal 
interactions across the microservices architecture.


<div align="center">
<a href="assets/Security.jpg"> <img alt="LiteMesh Security" src="assets/Security.jpg"></a>
</div>



## RESILIENCY

When an API call is routed through LiteMesh, it passes through several layers of resiliency mechanisms before reaching 
the final destination service. This flow ensures that the system can gracefully handle failures, traffic spikes, 
and timeouts. Here’s how it works:

1. **RedisRateLimiter:**
   ``
   The first layer of defense is the RedisRateLimiter, which manages the rate at which API requests
   are allowed to reach the microservices. Using Redis, the rate limiter stores the rate and burst capacities even in a
   distributed system, ensuring that limits are enforced consistently across multiple instances.
   ``

   * _How it works:_ Each API call consumes a token from a bucket (representing allowed requests). If the bucket is empty,
     the call is rejected with a 429 Too Many Requests error. This prevents overloading services during high traffic and
     protects against denial-of-service (DoS) attacks.
   * _Purpose:_ Controls traffic flow to prevent services from being overwhelmed by too many requests in a short time frame.
   

2. **TimeLimiter:**
   ``
   If the request passes the rate limiter, it then goes through the TimeLimiter. This mechanism imposes a
   maximum time for each request to complete.
   ``

   * _How it works:_ The TimeLimiter enforces a timeout duration for API calls. If a request exceeds this time limit 
   (e.g., due to network delays or slow services), it will be terminated, and an error will be returned to the client.
   * _Purpose:_ Prevents long-running requests from blocking system resources and ensures that slow services don't 
   degrade the overall performance of the system.
   

3. **CircuitBreaker:**
   ``
   After passing the time limit check, the request moves to the CircuitBreaker. This mechanism monitors
   the success and failure rates of API calls and trips the circuit (i.e., temporarily blocks further requests) when
   failures exceed a defined threshold.
   ``

   * _How it works:_ If a service starts failing consistently (e.g., due to a crash or a high error rate), the 
   CircuitBreaker will open, blocking new requests to that service until it recovers. This prevents additional strain 
   on the failing service and allows it time to recover. Once the system stabilizes, the CircuitBreaker transitions to 
   a half-open state, allowing limited traffic to pass through to test if the service has recovered.
   * _Purpose:_ Protects services from cascading failures and ensures that a service experiencing issues doesn’t affect 
   the rest of the system.


4. **Retry:**
   ``
   The Retry mechanism provides one last safety net by attempting to retry failed requests before giving up.
   If the request fails due to network issues or temporary errors, the Retry mechanism can re-attempt the call a specified
   number of times with a wait duration between retries.
   ``

   * _How it works:_ If a request fails due to transient errors (e.g., timeouts, network issues), the system 
   automatically retries the request up to a configurable number of attempts. Each retry is spaced out by a set wait 
   duration, allowing the system time to stabilize before trying again.
   * _Purpose:_ Improves resiliency by automatically recovering from temporary issues without requiring user intervention, 
   providing a smoother experience for clients.


<div align="center">
<a href="assets/Resiliency.jpg"> <img alt="LiteMesh Dynamic Routing" src="assets/Resiliency.jpg"></a>
</div>
