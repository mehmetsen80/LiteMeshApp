FROM quay.io/keycloak/keycloak:latest


## Stage 1: Use a lightweight builder image to install curl
#FROM alpine:latest as builder
#RUN apk add --no-cache curl
#
## Stage 2: Copy curl into the Keycloak image
#FROM quay.io/keycloak/keycloak:latest
#
#
## Build Stage 3
## Take the build from the previous stage
#FROM node:12-alpine
##Install curl
#RUN apk update && apk add curl
#
## Switch to root to allow copying and permission setting
#USER root
#
## Copy curl from the builder image to the Keycloak container
#COPY --from=builder /usr/bin/curl /usr/bin/curl
#
## Ensure curl has executable permissions
#RUN chmod +x /usr/bin/curl
#
## Switch back to the default non-root user Keycloak uses, if required
#USER 1000
