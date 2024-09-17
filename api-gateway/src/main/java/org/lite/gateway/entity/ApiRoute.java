package org.lite.gateway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("apiRoutes")
public record ApiRoute(@Id Integer Id, String routeIdentifier, String uri, String method, String path) {
}
