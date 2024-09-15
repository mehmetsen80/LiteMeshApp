package org.lite.mesh.enums;

public enum PreferredDiscovery {
    EUREKA("eureka"),
    NONE("none");

    private final String discovery;

    public String getDiscovery(){
        return discovery;
    }

    PreferredDiscovery(String discovery) {
        this.discovery = discovery;
    }
}
