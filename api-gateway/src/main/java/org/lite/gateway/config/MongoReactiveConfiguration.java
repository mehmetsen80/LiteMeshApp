package org.lite.gateway.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.lite.gateway.repository.**")
public class MongoReactiveConfiguration extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoDatabaseUri = "mongodb://root:mongopw@localhost";

    @Value("${spring.data.mongodb.database}")
    private String databaseName = "LiteMesh";

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Bean
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoDatabaseUri);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}