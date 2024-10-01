package org.lite.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@Document("apiRoutes")
@Data
public class ApiRoute{
    @Id String id;
    String routeIdentifier;
    String uri;
    String method;
    String path;
    List<FilterConfig> filters;
}


