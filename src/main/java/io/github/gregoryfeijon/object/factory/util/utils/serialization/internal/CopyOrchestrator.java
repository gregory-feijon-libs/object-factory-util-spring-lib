package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.FieldUtil;
import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Objects;

import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.defaultValueFor;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isClassMapCollection;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isPrimitiveOrEnum;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isWrapperType;

/**
 * Central dispatch for verifying and copying field values.
 * <p>
 * Handles type conversion scenarios (wrapper ↔ primitive, enum conversions)
 * and selects the appropriate cloning strategy (direct, serialization, or recursive).
 * </p>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CopyOrchestrator {

    /**
     * Verifies and processes the value to be copied, handling special cases where source and
     * destination field types differ.
     *
     * @param <S>         the type of the source object
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param source      the source object instance
     * @return the processed value ready to be set in the destination field
     */
    public static <S> Object verifyValue(Field sourceField, Field destField, S source) {
        Object rawValue = FieldUtil.getProtectedFieldValue(sourceField, source);
        Object sourceValue = HibernateProxyHandler.unproxyValueIfNeeded(rawValue);

        if (sourceField.getType() == destField.getType()) {
            return copyValue(sourceField, destField, sourceValue);
        }
        return convertBetweenTypes(sourceField, destField, sourceValue);
    }

    /**
     * Handles the copy when source and destination fields declare different types.
     */
    private static Object convertBetweenTypes(Field sourceField, Field destField, Object sourceValue) {
        Class<?> sourceType = sourceField.getType();
        Class<?> destType = destField.getType();

        if (isWrapperType(sourceType) && destType.isPrimitive() && sourceValue == null) {
            return defaultValueFor(destType);
        }
        if (isWrapperType(destType) && sourceType.isPrimitive()
                && Objects.equals(sourceValue, defaultValueFor(sourceType))) {
            return null;
        }
        if (sourceType.isEnum() || destType.isEnum()) {
            return EnumConverter.validateEnums(sourceField, destField, sourceValue);
        }
        if (isClassMapCollection(destType) || isClassMapCollection(sourceType)) {
            return null;
        }
        return copyValue(sourceField, destField, sourceValue);
    }

    /**
     * Determines the appropriate copying strategy based on the field types and copies the value.
     *
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param sourceValue the value to copy
     * @return the copied value
     */
    static Object copyValue(Field sourceField, Field destField, Object sourceValue) {
        Class<?> sourceFieldType = sourceField.getType();
        Class<?> destFieldType = destField.getType();

        if (isPrimitiveOrEnum(sourceFieldType)) {
            return sourceValue;
        }
        if (isWrapperType(sourceFieldType)) {
            return ObjectCloner.serializingClone(sourceValue, destFieldType);
        }
        if (isClassMapCollection(sourceFieldType)) {
            return CollectionMapCloner.serializingCloneCollectionMap(sourceValue, destField.getGenericType());
        }
        Class<?> effectiveType = TypeResolver.resolveEffectiveType(sourceValue, destFieldType);
        try {
            return ObjectCloner.serializingCloneObjects(sourceValue, effectiveType);
        } catch (Exception ex) {
            throw new ApiException(ex.getMessage(), ex);
        }
    }
}
