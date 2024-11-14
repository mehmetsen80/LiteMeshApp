# Dockerfile for api-gateway

# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-21-jammy AS build

# Set the working directory in the build image
WORKDIR /app

# Copy the entire project to the Docker container
COPY . .

# Build only the `api-gateway` module and package it as a JAR
RUN mvn clean install -pl api-gateway -am -DskipTests

# Stage 2: Create the runtime image with OpenJDK and application JAR
FROM eclipse-temurin:21-jdk-jammy

# Install necessary packages
RUN apt-get update && apt-get install -y net-tools && apt-get install -y openssl ca-certificates && rm -rf /var/lib/apt/lists/*

# Set the working directory in the runtime image
WORKDIR /app

# Copy the application JAR from the build image
COPY --from=build /app/api-gateway/target/LiteGateway.jar LiteGateway.jar

# Copy the keystore and truststore files
COPY keys/gateway-keystore-container.jks gateway-keystore.jks
COPY keys/gateway-truststore.jks gateway-truststore.jks

# Set permissions for keystore and truststore files
RUN chmod 644 /app/gateway-truststore.jks /app/gateway-keystore.jks

# Set environment variables for keystore and truststore
ENV GATEWAY_KEY_STORE=/app/gateway-keystore.jks
ENV GATEWAY_KEY_STORE_PASSWORD=123456
ENV GATEWAY_TRUST_STORE=/app/gateway-truststore.jks
ENV GATEWAY_TRUST_STORE_PASSWORD=123456

# Import all certificates from gateway-truststore.jks into the default cacerts truststore
RUN keytool -importkeystore -srckeystore /app/gateway-truststore.jks \
            -destkeystore "${JAVA_HOME}/lib/security/cacerts" \
            -srcstorepass "$GATEWAY_TRUST_STORE_PASSWORD" \
            -deststorepass "$GATEWAY_TRUST_STORE_PASSWORD" -noprompt -srcstoretype JKS -deststoretype JKS

# Set other service-specific environment variables
ENV REDIS_GATEWAY_URL=redis-service
ENV MONGO_GATEWAY_URL=mongodb-service
ENV KEYCLOAK_GATEWAY_URL=keycloak-service
ENV KEYCLOAK_GATEWAY_PORT=8080
ENV EUREKA_GATEWAY_URL=discovery-service

# Expose the API gateway port
EXPOSE 7777
#EXPOSE 2222


# Copy the wait script
#COPY .kube/gateway/wait-for-keycloak.sh /app/wait-for-keycloak.sh
#RUN chmod +x /app/wait-for-keycloak.sh

# Run the API gateway application with SSL configuration
#ENTRYPOINT ["/bin/sh", "-c", "/app/wait-for-keycloak.sh;", "java", "-Djavax.net.debug=all", "-Dhttps.protocols=TLSv1.2,TLSv1.3", "-Djavax.net.ssl.keyStore=${GATEWAY_KEY_STORE}", "-Djavax.net.ssl.keyStorePassword=${GATEWAY_KEY_STORE_PASSWORD}", "-Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts", "-Djavax.net.ssl.trustStorePassword=${GATEWAY_TRUST_STORE_PASSWORD}", "-jar", "LiteGateway.jar"]

# "-Dhttps.protocols=TLSv1.2,TLSv1.3",

ENTRYPOINT ["java", "-Djavax.net.debug=all", "-Djavax.net.debug=ssl", "-Dhttps.protocols=TLSv1.2,TLSv1.3", "-Djavax.net.ssl.keyStore=$GATEWAY_KEY_STORE", "-Djavax.net.ssl.keyStorePassword=$GATEWAY_KEY_STORE_PASSWORD", "-Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts", "-Djavax.net.ssl.trustStorePassword=$GATEWAY_TRUST_STORE_PASSWORD", "-jar", "LiteGateway.jar"]
