package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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
 * <p>
 * Constant lookups are memoized per enum class through a {@link ClassValue}, turning each
 * conversion into an O(1) map lookup without preventing class unloading.
 * </p>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumConverter {

    private static final ClassValue<Map<String, Object>> CONSTANTS_BY_NAME = new ClassValue<>() {
        @Override
        protected Map<String, Object> computeValue(Class<?> type) {
            Map<String, Object> constants = new HashMap<>();
            for (Object constant : type.getEnumConstants()) {
                constants.putIfAbsent(constant.toString(), constant);
            }
            return Map.copyOf(constants);
        }
    };

    /**
     * Validates and converts enum values between source and destination fields.
     *
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param sourceValue the value to convert
     * @return the converted value, or null if conversion is not possible
     */
    public static Object validateEnums(Field sourceField, Field destField, Object sourceValue) {
        Class<?> sourceType = sourceField.getType();
        Class<?> destType = destField.getType();

        if (destType.isEnum() && sourceType.equals(String.class)) {
            return findEnumConstantEquivalent(destType, sourceValue);
        }
        if (!sourceType.isEnum() || sourceValue == null) {
            return null;
        }
        if (destType.isEnum()) {
            return findEnumConstantEquivalent(destType, sourceValue.toString());
        }
        return destType.equals(String.class) ? sourceValue.toString() : null;
    }

    /**
     * Finds an enum constant equivalent to the given string value.
     *
     * @param type        the enum class to search
     * @param sourceValue the value to match (compared using toString())
     * @return the matching enum constant, or null if no match is found
     */
    static Object findEnumConstantEquivalent(Class<?> type, Object sourceValue) {
        return sourceValue instanceof String name ? CONSTANTS_BY_NAME.get(type).get(name) : null;
    }
}
