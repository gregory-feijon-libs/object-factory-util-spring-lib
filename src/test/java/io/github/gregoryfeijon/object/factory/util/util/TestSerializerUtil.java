package io.github.gregoryfeijon.object.factory.util.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.gregoryfeijon.serializer.provider.config.gson.adapter.OptionalTypeAdapter;
import io.github.gregoryfeijon.serializer.provider.config.gson.factory.EnumUseAttributeInMarshallingTypeAdapterFactory;
import io.github.gregoryfeijon.serializer.provider.config.gson.factory.GsonRemoveUnusedDecimalTypeAdapterFactory;
import io.github.gregoryfeijon.serializer.provider.config.gson.strategy.GsonSerializationStrategy;
import io.github.gregoryfeijon.serializer.provider.config.jackson.EnumCustomizationModule;
import io.github.gregoryfeijon.serializer.provider.config.jackson.factory.EnumDeserializers;
import io.github.gregoryfeijon.serializer.provider.config.jackson.factory.EnumSerializers;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Utility class for configuring test serializers (Jackson and Gson).
 * <p>
 * This class provides pre-configured ObjectMapper and Gson instances
 * for use in tests, ensuring consistency with production configuration.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestSerializerUtil {

    @Getter
    private static final ObjectMapper objectMapper;

    @Getter
    private static final Gson gson;

    static {
        objectMapper = buildObjectMapper();
        gson = buildGsonGmt();
    }

    /**
     * Builds an ObjectMapper with custom enum serialization module.
     * <p>
     * This method reuses the same {@link EnumCustomizationModule} that is used
     * in the Spring Boot auto-configuration, ensuring test and production
     * configurations are identical.
     *
     * @return Configured ObjectMapper instance
     */
    public static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java Time module for date/time handling
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);

        // Register the custom enum module (same as production)
        EnumCustomizationModule enumModule = new EnumCustomizationModule(
                new EnumSerializers(),
                new EnumDeserializers()
        );
        mapper.registerModule(enumModule);

        // Configure serialization features
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }

    /**
     * Builds a Gson instance with custom adapters and exclusion strategies.
     *
     * @return Configured Gson instance
     */
    public static Gson buildGsonGmt() {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Optional.class, new OptionalTypeAdapter())
                .registerTypeAdapterFactory(new EnumUseAttributeInMarshallingTypeAdapterFactory())
                .registerTypeAdapterFactory(new GsonRemoveUnusedDecimalTypeAdapterFactory())
                .addSerializationExclusionStrategy(new GsonSerializationStrategy());

        return builder.create();
    }
}
