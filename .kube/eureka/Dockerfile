FROM maven:3.9.8-eclipse-temurin-21-jammy AS build

# Set the working directory in the build image
WORKDIR /app

# Copy the entire project to the Docker container
COPY . .

# Build only the `discovery-server` module and package it as a JAR
RUN mvn clean install -pl discovery-server -am -DskipTests

# Stage 2: Create the runtime image with OpenJDK and application JAR
FROM eclipse-temurin:21-jdk-jammy

# Install necessary packages
RUN apt-get update && apt-get install -y net-tools && apt-get install -y openssl ca-certificates && rm -rf /var/lib/apt/lists/*

# Set the working directory in the runtime image
WORKDIR /app

# Copy the application JAR from the build image
COPY --from=build /app/discovery-server/target/DiscoveryService.jar DiscoveryService.jar

# Copy keystore and truststore
COPY keys/eureka-keystore-container.jks eureka-keystore.jks
COPY keys/eureka-truststore.jks eureka-truststore.jks

# Set permissions for the keystore and truststore files
RUN chmod 644 /app/eureka-truststore.jks /app/eureka-keystore.jks

# Define the environment variables for keystore and truststore
ENV EUREKA_KEY_STORE=/app/eureka-keystore.jks
ENV EUREKA_KEY_STORE_PASSWORD=123456
ENV EUREKA_TRUST_STORE=/app/eureka-truststore.jks
ENV EUREKA_TRUST_STORE_PASSWORD=123456
ENV EUREKA_ALIAS_NAME=eureka-app-container

# Expose the secure port for Eureka
EXPOSE 8761

# Import all certificates from eureka-truststore.jks into the default cacerts
RUN keytool -importkeystore -srckeystore /app/eureka-truststore.jks \
            -destkeystore "${JAVA_HOME}/lib/security/cacerts" \
            -srcstorepass "$EUREKA_TRUST_STORE_PASSWORD" \
            -deststorepass "$EUREKA_TRUST_STORE_PASSWORD" -noprompt -srcstoretype JKS -deststoretype JKS

# Run the Eureka server with SSL configuration
ENTRYPOINT ["java", "-Djavax.net.debug=all", "-Dhttps.protocols=TLSv1.2,TLSv1.3", "-Djavax.net.debug=ssl:handshake", "-Djavax.net.ssl.keyStore=${EUREKA_KEY_STORE}", "-Djavax.net.ssl.keyStoreType=JKS", "-Djavax.net.ssl.keyStorePassword=$EUREKA_KEY_STORE_PASSWORD", "-Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts", "-Djavax.net.ssl.trustStorePassword=$EUREKA_TRUST_STORE_PASSWORD", "-jar", "DiscoveryService.jar"]
