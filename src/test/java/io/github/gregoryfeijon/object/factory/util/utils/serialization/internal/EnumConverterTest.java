package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestDest;
import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EnumConverterTest {

    @SuppressWarnings("unused")
    private StatusTestSource sourceEnum = StatusTestSource.ACTIVE;
    @SuppressWarnings("unused")
    private StatusTestDest destEnum;
    @SuppressWarnings("unused")
    private String stringField;
    @SuppressWarnings("unused")
    private Integer integerField;

    private Field field(String name) throws NoSuchFieldException {
        return EnumConverterTest.class.getDeclaredField(name);
    }

    @Test
    void validateEnums_shouldConvertStringToEnum() throws Exception {
        Field source = field("stringField");
        Field dest = field("destEnum");

        Object result = EnumConverter.validateEnums(source, dest, "ACTIVE");

        assertThat(result).isEqualTo(StatusTestDest.ACTIVE);
    }

    static Stream<Arguments> validateEnums_shouldReturnNullProvider() throws NoSuchFieldException {
        Field stringField = EnumConverterTest.class.getDeclaredField("stringField");
        Field destEnum = EnumConverterTest.class.getDeclaredField("destEnum");
        Field sourceEnum = EnumConverterTest.class.getDeclaredField("sourceEnum");
        Field integerField = EnumConverterTest.class.getDeclaredField("integerField");

        return Stream.of(
                Arguments.of("string does not match enum", stringField, destEnum, "NONEXISTENT"),
                Arguments.of("enum to string but source value is null", sourceEnum, stringField, null),
                Arguments.of("non-enum types", stringField, integerField, "test"),
                Arguments.of("enum to enum but source value is null", sourceEnum, destEnum, null)
        );
    }

    @ParameterizedTest(name = "shouldReturnNull when {0}")
    @MethodSource("validateEnums_shouldReturnNullProvider")
    void validateEnums_shouldReturnNull(String scenario, Field source, Field dest, Object value) {
        Object result = EnumConverter.validateEnums(source, dest, value);

        assertThat(result).isNull();
    }

    @Test
    void validateEnums_shouldConvertEnumToEnum() throws Exception {
        Field source = field("sourceEnum");
        Field dest = field("destEnum");

        Object result = EnumConverter.validateEnums(source, dest, StatusTestSource.ACTIVE);

        assertThat(result).isEqualTo(StatusTestDest.ACTIVE);
    }

    @Test
    void validateEnums_shouldConvertEnumToString() throws Exception {
        Field source = field("sourceEnum");
        Field dest = field("stringField");

        Object result = EnumConverter.validateEnums(source, dest, StatusTestSource.ACTIVE);

        assertThat(result).isEqualTo("ACTIVE");
    }

    @Test
    void findEnumConstantEquivalent_shouldFindMatchingConstant() {
        Object result = EnumConverter.findEnumConstantEquivalent(StatusTestDest.class, "INACTIVE");

        assertThat(result).isEqualTo(StatusTestDest.INACTIVE);
    }

    @Test
    void findEnumConstantEquivalent_shouldReturnNullWhenNoMatch() {
        Object result = EnumConverter.findEnumConstantEquivalent(StatusTestDest.class, "UNKNOWN");

        assertThat(result).isNull();
    }
}
