package org.lite.gateway.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.lang.NonNull;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.lite.gateway.repository")
@EnableReactiveMongoAuditing
public class MongoReactiveConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected @NonNull String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public @NonNull MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public @NonNull ReactiveMongoTemplate reactiveMongoTemplate() {
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setMapKeyDotReplacement("_");
        return template;
    }

    //Very important, do not remove this, otherwise the HealthCheckConfig "save" throws error although it really saves
    @Bean
    @Override
    public @NonNull MappingMongoConverter mappingMongoConverter(
            @NonNull ReactiveMongoDatabaseFactory databaseFactory,
            @NonNull MongoCustomConversions customConversions,
            @NonNull MongoMappingContext mappingContext) {
            
        MappingMongoConverter converter = super.mappingMongoConverter(databaseFactory, customConversions, mappingContext);
        converter.setMapKeyDotReplacement("_");  // Replace dots with underscores in map keys
        return converter;
    }

//    @Bean
//    public MongoMappingContext mongoMappingContext(MongoCustomConversions conversions) {
//        MongoMappingContext mappingContext = new MongoMappingContext();
//        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
//        mappingContext.setFieldNamingStrategy(new DotReplacementFieldNamingStrategy());
//        return mappingContext;
//    }
//
//    private static class DotReplacementFieldNamingStrategy implements FieldNamingStrategy {
//        @Override
//        public @NonNull String getFieldName(@NonNull PersistentProperty<?> property) {
//            String name = property.getName();
//            return name.replace(".", "_");
//        }
//    }
}