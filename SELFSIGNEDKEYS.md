# GENERATE TRUSTSTORES
We self-signed the keys for local run

Assuming you are under the root project, you can list the files under "keys" folder we have generated so far
```text 
$ ls -ltr keys/
```
```text 
-rw-r--r--@ 1 mehmetsen  staff  2736 Nov 11 21:53 client-keystore.jks
-rw-r--r--@ 1 mehmetsen  staff   879 Nov 11 21:53 client-cert.pem
-rw-r--r--@ 1 mehmetsen  staff  2818 Nov 11 21:54 inventory-keystore-container.jks
-rw-r--r--@ 1 mehmetsen  staff   895 Nov 11 21:54 inventory-cert-container.pem
-rw-r--r--@ 1 mehmetsen  staff  2814 Nov 11 21:54 product-keystore-container.jks
-rw-r--r--@ 1 mehmetsen  staff   892 Nov 11 21:54 product-cert-container.pem
-rw-r--r--@ 1 mehmetsen  staff  2738 Nov 11 21:54 gateway-keystore.jks
-rw-r--r--@ 1 mehmetsen  staff   879 Nov 11 21:55 gateway-cert.pem
-rw-r--r--@ 1 mehmetsen  staff  2806 Nov 11 21:55 gateway-keystore-container.jks
-rw-r--r--@ 1 mehmetsen  staff   900 Nov 11 21:55 gateway-cert-container.pem
-rw-r--r--@ 1 mehmetsen  staff  2736 Nov 11 21:55 eureka-keystore.jks
-rw-r--r--@ 1 mehmetsen  staff   880 Nov 11 21:55 eureka-cert.pem
-rw-r--r--@ 1 mehmetsen  staff  2788 Nov 11 21:55 eureka-keystore-container.jks
-rw-r--r--@ 1 mehmetsen  staff   895 Nov 11 21:56 eureka-cert-container.pem
-rw-r--r--@ 1 mehmetsen  staff  7366 Nov 11 21:57 gateway-truststore.jks
-rw-r--r--@ 1 mehmetsen  staff  7366 Nov 11 21:59 client-truststore.jks
-rw-r--r--@ 1 mehmetsen  staff  5350 Nov 11 22:00 eureka-truststore.jks
```

## GENERATE CERTIFICATES

Now let's explain how we generated the keys. 

### CLIENT (INVENTORY-SERVICE, PRODUCT-SERVICE etc.)
Client certificate is a generic one that represents all micro-services.

Generate the client keystore
```text 
$ keytool -genkeypair -alias client-app -keyalg RSA -keysize 2048 -keystore client-keystore.jks -validity 3650  -storetype PKCS12 -dname "CN=localhost, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export the Client’s certificate from the keystore
```text 
$ keytool -exportcert -alias client-app -file client-cert.pem -keystore client-keystore.jks -storepass 123456
```



### INVENTORY-SERVICE
inventory-service and product-service keystores are used only in the containerized environment.
You only need one client truststore if you are running as standalone.

So, we need to create the certs separately for each client for containerized environment.
```text 
$ keytool -genkeypair -alias inventory-service-container -keyalg RSA -keysize 2048 -keystore inventory-keystore-container.jks -validity 3650  -storetype PKCS12 -dname "CN=inventory-service, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export certificate from keystore
```text 
$ keytool -exportcert -alias inventory-service-container -file inventory-cert-container.pem -keystore inventory-keystore-container.jks -storepass 123456
```


### PRODUCT-SERVICE
We realized that there is need to create the certs separately for each client
```text 
$ keytool -genkeypair -alias product-service-container -keyalg RSA -keysize 2048 -keystore product-keystore-container.jks -validity 3650  -storetype PKCS12 -dname "CN=product-service, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export certificate from keystore (adding to the same client-cert.pem)
```text 
$ keytool -exportcert -alias product-service-container -file product-cert-container.pem -keystore product-keystore-container.jks -storepass 123456
```


### SERVER (GATEWAY)
Generate the gateway keystore
```text 
$ keytool -genkeypair -alias gateway-app -keyalg RSA -keysize 2048  -keystore gateway-keystore.jks -validity 3650  -storetype PKCS12 -dname "CN=localhost, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export Gateway’s Certificate
```text 
$ keytool -exportcert -alias gateway-app -file gateway-cert.pem -keystore gateway-keystore.jks -storepass 123456
```


### GATEWAY CERT FOR DOCKERIZED ENVIRONMENT
If you are using a containerized gateway then follow the below
Generate keystore for container:
```text 
$ keytool -genkeypair -alias gateway-app-container -keyalg RSA -keysize 2048  -keystore gateway-keystore-container.jks -validity 3650  -storetype PKCS12 -dname "CN=api-gateway-service, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export certificate
```text 
$ keytool -exportcert -alias gateway-app-container -file gateway-cert-container.pem -keystore gateway-keystore-container.jks -storepass 123456
```


### EUREKA DISCOVERY
Generate the discovery keystore
```text 
$ keytool -genkeypair -alias eureka-app -keyalg RSA -keysize 2048 -keystore eureka-keystore.jks -validity 3650  -storetype PKCS12 -dname "CN=localhost, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export the Eureka’s certificate from the keystore
```text 
$ keytool -exportcert -alias eureka-app -file eureka-cert.pem -keystore eureka-keystore.jks -storepass 123456
```

### EUREKA CERT FOR DOCKERIZED ENVIRONMENT
We need to also create a certificate specifically for the container as the container sees the hostname (CN) from “discovery-service” (Let’s name it eureka-app-container and eureka-keystone-container.jks)
```text 
$ keytool -genkeypair -alias eureka-app-container -keyalg RSA -keysize 2048 -keystore eureka-keystore-container.jks -validity 3650 -storetype PKCS12 -dname "CN=discovery-service, OU=Software, O=Dipme, L=Richmond, ST=TX, C=US" -storepass 123456 -keypass 123456
```
Export the certificate (Let’s name it eureka-cert-container.pem)
```text 
$ keytool -exportcert -alias eureka-app-container -file eureka-cert-container.pem -keystore eureka-keystore-container.jks -storepass 123456
```


### OTHER COMMANDS
Run the Eureka Server and verify the certificate chain of your server’s certificate
```text 
$ openssl s_client -showcerts -connect localhost:8761
```


## IMPORT CERTIFICATES
Now it's time to import the certificates into truststores. Remember that we also import the gateway certificate into its truststore.

### IMPORT ALL CERTIFICATES INTO THE GATEWAY TRUSTSTORE
```text 
$ keytool -importcert -file gateway-cert.pem -alias gateway-app -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file gateway-cert-container.pem -alias gateway-app-container -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file client-cert.pem -alias client-app -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file inventory-cert-container.pem -alias inventory-service-container -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file product-cert-container.pem -alias product-service-container -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file eureka-cert.pem -alias eureka-app  -keystore gateway-truststore.jks -storepass 123456
$ keytool -importcert -file eureka-cert-container.pem -alias eureka-app-container -keystore gateway-truststore.jks -storepass 123456
```

## TO LIST THE CONTENT OF THE GATEWAY JKS FILE
```text 
$ keytool -list -v -keystore gateway-truststore.jks -storepass 123456
```
You’ll see 7 different alias names and pems


### IMPORT ALL CERTIFICATES INTO THE CLIENT TRUSTSTORE
```text 
$ keytool -importcert -file client-cert.pem -alias client-app  -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file inventory-cert-container.pem -alias inventory-service-container  -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file product-cert-container.pem -alias product-service-container  -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file gateway-cert.pem -alias gateway-app -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file gateway-cert-container.pem -alias gateway-app-container -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file eureka-cert.pem -alias eureka-app  -keystore client-truststore.jks -storepass 123456
$ keytool -importcert -file eureka-cert-container.pem -alias eureka-app-container  -keystore client-truststore.jks -storepass 123456
```

## TO LIST THE CONTENT OF THE GATEWAY JKS FILE
```text 
$ keytool -list -v -keystore client-truststore.jks -storepass 123456
```
You’ll see 7 different alias names and pems

### IMPORT ALL CERTIFICATES INTO THE EUREKA TRUSTSTORE
```text 
$ keytool -importcert -file gateway-cert.pem -alias gateway-app -keystore eureka-truststore.jks -storepass 123456
$ keytool -importcert -file client-cert.pem -alias client-app -keystore eureka-truststore.jks -storepass 123456
$ keytool -importcert -file inventory-cert-container.pem -alias inventory-service-container -keystore eureka-truststore.jks -storepass 123456
$ keytool -importcert -file product-cert-container.pem -alias product-service-container -keystore eureka-truststore.jks -storepass 123456
$ keytool -importcert -file gateway-cert-container.pem -alias gateway-app-container -keystore eureka-truststore.jks -storepass 123456
```

## TO LIST THE CONTENT OF THE JKS FILE
```text 
$ keytool -list -v -keystore eureka-truststore.jks -storepass 123456
```
You’ll see 5 different alias names and pems
