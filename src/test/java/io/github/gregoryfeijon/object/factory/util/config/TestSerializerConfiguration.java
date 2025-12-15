package io.github.gregoryfeijon.object.factory.util.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.gregoryfeijon.object.factory.util.util.TestSerializerUtil;
import io.github.gregoryfeijon.serializer.provider.domain.properties.SerializerProviderProperties;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSerializerConfiguration {

    @Bean
    public ObjectMapper testObjectMapper() {
        return TestSerializerUtil.buildObjectMapper();
    }

    @Bean
    public Gson testGson() {
        return TestSerializerUtil.buildGsonGmt();
    }

    @Bean("gsonUtc")
    public Gson gsonUtc() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
    }

    @Bean("gsonBrasilia")
    public Gson gsonBrasilia() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
    }

    @Bean
    public SerializerProviderProperties serializerProviderProperties() {
        SerializerProviderProperties props = new SerializerProviderProperties();
        props.setEnabled(true);
        props.setDefaultGsonBean("testGson");
        return props;
    }

    @Bean
    public ApplicationRunner serializerProviderTestInitializer() {
        return args -> {
            // Garante inicialização previsível nos testes
            SerializerProvider.initializeIfEmpty();
        };
    }
}
