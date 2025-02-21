package org.lite.gateway.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class ServiceUsageAggregation {
    @Field("_id")
    private String id;
    private Long requestCount;
} 