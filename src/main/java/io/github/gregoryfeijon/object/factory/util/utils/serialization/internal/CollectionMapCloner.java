package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.object.factory.util.exception.ErrorMessages;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import io.github.gregoryfeijon.serializer.provider.util.gson.GsonTypesUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanInstantiationException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.getRawType;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isClassMapCollection;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isCollection;

/**
 * Deep copies collections and maps, preserving generic type information
 * and handling element type conversion when source and destination types differ.
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectionMapCloner {

    /**
     * Creates a deep copy of a collection or map via serialization, preserving generic type information.
     *
     * @param sourceValue the collection or map to clone
     * @param genericType the generic type information of the target field
     * @return a deep copy of the source value, or null if the source value is null
     */
    public static Object serializingCloneCollectionMap(Object sourceValue, Type genericType) {
        if (sourceValue == null) {
            return null;
        }
        try {
            return isCollection(sourceValue.getClass())
                    ? cloneCollection((Collection<?>) sourceValue, genericType)
                    : cloneMap((Map<?, ?>) sourceValue, genericType);
        } catch (Exception ex) {
            throw new ApiException(ErrorMessages.CLONE_COLLECTION_MAP_ERROR, ex);
        }
    }

    // ---------------------------------------------------------------- collections

    private static Object cloneCollection(Collection<?> source, Type genericType) throws ClassNotFoundException {
        if (source.isEmpty()) {
            return ObjectCloner.cloneToJsonFormat(Collections.emptyList(), genericType);
        }
        Object firstElement = source.iterator().next();
        Class<?> targetElementType = TypeResolver.extractCollectionElementType(genericType);

        if (isClassMapCollection(firstElement.getClass())) {
            return cloneNestedCollection(source, genericType, firstElement, targetElementType);
        }
        return cloneSimpleCollection(source, genericType, firstElement.getClass(), targetElementType);
    }

    private static Object cloneNestedCollection(Collection<?> source, Type genericType,
                                                Object firstElement, Class<?> targetElementType)
            throws ClassNotFoundException {
        if (TypeResolver.getInnermostElementType(firstElement) == targetElementType) {
            return deserializeWithFallback(source, genericType);
        }
        Type nestedType = TypeResolver.getNestedGenericType(genericType, 0);
        return mapElements(source, item -> serializingCloneCollectionMap(item, nestedType));
    }

    private static Object cloneSimpleCollection(Collection<?> source, Type genericType,
                                                Class<?> sourceElementType, Class<?> targetElementType)
            throws ClassNotFoundException {
        Class<?> effectiveType = TypeResolver.resolveEffectiveType(sourceElementType, targetElementType);
        if (sourceElementType != effectiveType) {
            return mapElements(source, item -> ObjectFactoryUtil.createFromObject(item, effectiveType));
        }
        if (effectiveType != targetElementType) {
            Type concreteType = GsonTypesUtil.getType(getRawType(genericType), effectiveType);
            return ObjectCloner.cloneToJsonFormat(source, concreteType);
        }
        return deserializeWithFallback(source, genericType);
    }

    /**
     * Deserializes using the declared generic type; when that type cannot be instantiated,
     * falls back to the runtime type of the first element.
     */
    private static Object deserializeWithFallback(Collection<?> source, Type genericType)
            throws ClassNotFoundException {
        try {
            TypeResolver.verifyType(genericType);
            return ObjectCloner.cloneToJsonFormat(source, genericType);
        } catch (BeanInstantiationException | ClassNotFoundException ex) {
            Class<?> runtimeElementType = source.iterator().next().getClass();
            Type fallbackType = GsonTypesUtil.getType(getRawType(genericType), runtimeElementType);
            return ObjectCloner.cloneToJsonFormat(source, fallbackType);
        }
    }

    private static List<Object> mapElements(Collection<?> source, UnaryOperator<Object> converter) {
        List<Object> converted = new ArrayList<>(source.size());
        source.forEach(item -> converted.add(converter.apply(item)));
        return converted;
    }

    // ---------------------------------------------------------------- maps

    private static Object cloneMap(Map<?, ?> source, Type genericType) throws ClassNotFoundException {
        if (source.isEmpty()) {
            return ObjectCloner.cloneToJsonFormat(Collections.emptyMap(), genericType);
        }
        Object firstValue = source.values().iterator().next();
        Class<?> targetValueType = TypeResolver.extractMapValueType(genericType);

        if (isClassMapCollection(firstValue.getClass())) {
            return cloneNestedMap(source, genericType, firstValue, targetValueType);
        }
        return cloneSimpleMap(source, genericType, firstValue.getClass(), targetValueType);
    }

    private static Object cloneNestedMap(Map<?, ?> source, Type genericType,
                                         Object firstValue, Class<?> targetValueType) {
        if (TypeResolver.getInnermostElementType(firstValue) == targetValueType) {
            return ObjectCloner.cloneToJsonFormat(source, genericType);
        }
        Type nestedType = TypeResolver.getNestedGenericType(genericType, 1);
        return mapValues(source, value -> serializingCloneCollectionMap(value, nestedType));
    }

    private static Object cloneSimpleMap(Map<?, ?> source, Type genericType,
                                         Class<?> sourceValueType, Class<?> targetValueType)
            throws ClassNotFoundException {
        Class<?> effectiveType = TypeResolver.resolveEffectiveType(sourceValueType, targetValueType);
        if (sourceValueType != effectiveType) {
            return mapValues(source, value -> ObjectFactoryUtil.createFromObject(value, effectiveType));
        }
        if (effectiveType != targetValueType) {
            Class<?> keyType = getRawType(TypeResolver.getNestedGenericType(genericType, 0));
            Type concreteType = GsonTypesUtil.getType(getRawType(genericType), keyType, effectiveType);
            return ObjectCloner.cloneToJsonFormat(source, concreteType);
        }
        return ObjectCloner.cloneToJsonFormat(source, genericType);
    }

    private static Map<Object, Object> mapValues(Map<?, ?> source, UnaryOperator<Object> converter) {
        Map<Object, Object> converted = HashMap.newHashMap(source.size());
        source.forEach((key, value) -> converted.put(key, converter.apply(value)));
        return converted;
    }
}
