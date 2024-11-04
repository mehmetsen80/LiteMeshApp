# Dockerfile for discovery-server (Eureka)

# Stage 1: Build the application using Maven
#FROM maven:3.8.7-openjdk-21-slim AS build
FROM maven:3.9.8-eclipse-temurin-21 AS build

# Set the working directory in the build image
WORKDIR /app

# Copy the entire project to the Docker container
COPY . .

# Build only the `discovery-server` module and package it as a JAR
RUN mvn clean install -pl discovery-server -am -DskipTests

# Stage 2: Create the runtime image with OpenJDK and application JAR
FROM openjdk:21-slim

# Set the working directory in the runtime image
WORKDIR /app

# Copy the application JAR from the build image
COPY --from=build /app/discovery-server/target/DiscoveryService.jar DiscoveryService.jar

# Copy the keystore file from the build context
COPY keys/eureka-keystore.jks eureka-keystore.jks

# Set environment variables for keystore
ENV EUREKA_KEY_STORE=eureka-keystore.jks
ENV EUREKA_KEY_STORE_PASSWORD=123456

# Expose the secure port for Eureka
EXPOSE 8761
EXPOSE 443

# Run the Eureka server with SSL configuration
ENTRYPOINT ["java", "-Djavax.net.ssl.keyStore=${EUREKA_KEY_STORE}", "-Djavax.net.ssl.keyStorePassword=${EUREKA_KEY_STORE_PASSWORD}", "-jar", "DiscoveryService.jar"]
