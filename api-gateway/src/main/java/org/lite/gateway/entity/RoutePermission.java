package org.lite.gateway.entity;

public enum RoutePermission {
    VIEW,           // Can see the route exists
    EXECUTE,        // Can make API calls
    MANAGE,         // Can modify route settings
    MONITOR        // Can view metrics and health
} 