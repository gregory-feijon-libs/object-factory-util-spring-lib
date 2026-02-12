package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Handles enum type conversions during object copy operations.
 * <p>
 * Supports the following conversion scenarios:
 * </p>
 * <ul>
 *   <li>String → Enum: finds the constant matching the string value</li>
 *   <li>Enum → String: converts to string representation</li>
 *   <li>Enum → Enum: converts between different enum types by matching names</li>
 * </ul>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumConverter {

    /**
     * Validates and converts enum values between source and destination fields.
     *
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param sourceValue the value to convert
     * @return the converted value, or null if conversion is not possible
     */
    public static Object validateEnums(Field sourceField, Field destField, Object sourceValue) {
        Class<?> sourceFieldType = sourceField.getType();
        Class<?> destFieldType = destField.getType();

        if (destFieldType.isEnum()) {
            if (sourceFieldType.equals(String.class)) {
                return findEnumConstantEquivalent(destFieldType, sourceValue);
            } else if (sourceFieldType.isEnum() && sourceValue != null) {
                return findEnumConstantEquivalent(destFieldType, sourceValue.toString());
            }
        }
        if (sourceFieldType.isEnum() && (sourceValue != null && destFieldType.equals(String.class))) {
            return sourceValue.toString();
        }
        return null;
    }

    /**
     * Finds an enum constant equivalent to the given string value.
     *
     * @param type        the enum class to search
     * @param sourceValue the value to match (compared using toString())
     * @return the matching enum constant, or null if no match is found
     */
    static Object findEnumConstantEquivalent(Class<?> type, Object sourceValue) {
        return Stream.of(type.getEnumConstants())
                .filter(enumConstant -> Objects.equals(enumConstant.toString(), sourceValue))
                .findFirst()
                .orElse(null);
    }
}
