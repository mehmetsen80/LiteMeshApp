# Dockerfile for product-service

# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-21-jammy AS build

# Set the working directory in the build image
WORKDIR /app

# Copy the entire project to the Docker container
COPY . .

# Build only the `product-service` module and package it as a JAR
RUN mvn clean install -pl product-service -am -DskipTests

# Stage 2: Create the runtime image with OpenJDK and application JAR
FROM eclipse-temurin:21-jdk-jammy

# Install necessary packages
RUN apt-get update && apt-get install -y net-tools && apt-get install -y openssl ca-certificates && rm -rf /var/lib/apt/lists/*

# Set the working directory in the runtime image
WORKDIR /app

# Copy the application JAR from the build image
COPY --from=build /app/product-service/target/ProductService.jar ProductService.jar

# Copy the keystore and truststore files
COPY keys/product-keystore-container.jks client-keystore.jks
COPY keys/client-truststore.jks client-truststore.jks

# Set permissions for keystore and truststore files
RUN chmod 644 /app/client-truststore.jks /app/client-keystore.jks

# Set environment variables for keystore and truststore
ENV CLIENT_KEY_STORE=/app/client-keystore.jks
ENV CLIENT_KEY_STORE_PASSWORD=123456
ENV CLIENT_TRUST_STORE=/app/client-truststore.jks
ENV CLIENT_TRUST_STORE_PASSWORD=123456
ENV GATEWAY_SERVICE_URL=api-gateway-service

# Import all certificates from client-truststore.jks into the default cacerts truststore
RUN keytool -importkeystore -srckeystore /app/client-truststore.jks \
            -destkeystore "${JAVA_HOME}/lib/security/cacerts" \
            -srcstorepass "${CLIENT_TRUST_STORE_PASSWORD}" \
            -deststorepass "${CLIENT_TRUST_STORE_PASSWORD}" -noprompt -srcstoretype JKS -deststoretype JKS

# Run the API product-service application with SSL configuration
ENTRYPOINT ["java", "-Dhttps.protocols=TLSv1.2,TLSv1.3",  "-Djavax.net.ssl.keyStore=${CLIENT_KEY_STORE}", "-Djavax.net.ssl.keyStorePassword=${CLIENT_KEY_STORE_PASSWORD}", "-Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts", "-Djavax.net.ssl.trustStorePassword=${CLIENT_TRUST_STORE_PASSWORD}", "-jar", "ProductService.jar"]
