# Dockerfile for api-gateway

# Use a minimal OpenJDK image
FROM openjdk:21-slim

# Copy the application JAR file and key/truststore files into the image
#ADD /api-gateway/target/LiteGateway.jar LiteGateway.jar
#ADD /api-gateway/keys/gateway-keystore.jks gateway-keystore.jks
#ADD /api-gateway/keys/gateway-truststore.jks gateway-truststore.jks

ADD target/LiteGateway.jar LiteGateway.jar
ADD keys/gateway-keystore.jks gateway-keystore.jks
ADD keys/gateway-truststore.jks gateway-truststore.jks

# Copy and give execute permission to the wait script
#COPY wait-for.sh /wait-for.sh
#RUN chmod +x /wait-for.sh

# Set environment variables for the key store and trust store and password
# Replace later with actual password or use a secret management strategy
ENV GATEWAY_TRUST_STORE=gateway-truststore.jks
ENV GATEWAY_TRUST_STORE_PASSWORD=123456
ENV GATEWAY_KEY_STORE=gateway-keystore.jks
ENV GATEWAY_KEY_STORE_PASSWORD=123456


EXPOSE 7777
##EXPOSE 6379
##EXPOSE 27017
#
EXPOSE 8281
EXPOSE 8761
EXPOSE 443
EXPOSE 8080

# Run the application with SSL configuration
#ENTRYPOINT ["/wait-for.sh", "mongodb:27017", "java", "-Djavax.net.ssl.trustStore=${GATEWAY_TRUST_STORE}", "-Djavax.net.ssl.trustStorePassword=${GATEWAY_TRUST_STORE_PASSWORD}", "-jar", "LiteGateway.jar"]

ENTRYPOINT ["java", "-Djavax.net.debug=ssl:handshake", "-Djavax.net.ssl.trustStore=${GATEWAY_TRUST_STORE}", "-Djavax.net.ssl.trustStorePassword=${GATEWAY_TRUST_STORE_PASSWORD}", "-Djavax.net.ssl.keyStore=${GATEWAY_KEY_STORE}", "-Djavax.net.ssl.keyStorePassword=${GATEWAY_KEY_STORE_PASSWORD}", "-Dcom.sun.net.ssl.checkRevocation=false", "-jar", "LiteGateway.jar"]

#RUN java -Djavax.net.ssl.trustStore=${GATEWAY_TRUST_STORE}  -Djavax.net.ssl.trustStorePassword=${GATEWAY_TRUST_STORE_PASSWORD} -jar LiteGateway.jar