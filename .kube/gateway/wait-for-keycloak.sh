##!/bin/sh
#
## URL of the Keycloak health check
#KEYCLOAK_HEALTH_URL="http://${KEYCLOAK_GATEWAY_URL}:9000/health/ready"
#
#
### Wait until Keycloak becomes healthy
##echo "Waiting for Keycloak to be ready at ${KEYCLOAK_HEALTH_URL}..."
##until echo ${KEYCLOAK_HEALTH_URL} & curl --head -sf "$KEYCLOAK_HEALTH_URL"; do
##  echo "Keycloak is not ready. Retrying in 5 seconds..."
##  sleep 5
##done
##
##echo "Keycloak is ready. Starting API gateway..."
##java -Djavax.net.debug=all -Dhttps.protocols=TLSv1.2,TLSv1.3 \
##    -Djavax.net.ssl.keyStore=${GATEWAY_KEY_STORE} \
##    -Djavax.net.ssl.keyStorePassword=${GATEWAY_KEY_STORE_PASSWORD} \
##    -Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts \
##    -Djavax.net.ssl.trustStorePassword=${GATEWAY_TRUST_STORE_PASSWORD} \
##    -jar LiteGateway.jar
