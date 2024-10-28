# INTELLIJ SETTINGS

## RUN THE JAR FILES FROM INTELLIJ
Ensure your keystore and truststore files are located in a designated folder of your choice. In this example, they’re placed in my project folder:

/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/

Your path may vary depending on your setup.

To run the applications in IntelliJ, follow the configurations below:

#### discovery-server: (Only EV)
Environmental Variables (EV):
```shell 
EUREKA_KEY_STORE=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/eureka-keystore.jks;EUREKA_KEY_STORE_PASSWORD=123456
```

#### api-gateway:
VM Options:
```shell 
-Djavax.net.ssl.trustStore=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/gateway-truststore.jks -Djavax.net.ssl.trustStorePassword=123456
```

Environmental Variables (EV):
```shell 
GATEWAY_KEY_STORE=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/gateway-keystore.jks;GATEWAY_KEY_STORE_PASSWORD=123456;GATEWAY_TRUST_STORE=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/gateway-truststore.jks;GATEWAY_TRUST_STORE_PASSWORD=123456
```

#### Inventory, Product and Mesh Service:
VM Options:
```shell 
-Djavax.net.ssl.trustStore=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/client-truststore.jks -Djavax.net.ssl.trustStorePassword=123456
```

Environmental Variables (EV):
```shell 
CLIENT_KEY_STORE=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/client-keystore.jks;CLIENT_KEY_STORE_PASSWORD=123456;CLIENT_TRUST_STORE=/Users/mehmetsen/IdeaProjects/LiteMeshApp/keys/client-truststore.jks;CLIENT_TRUST_STORE_PASSWORD=123456
```
