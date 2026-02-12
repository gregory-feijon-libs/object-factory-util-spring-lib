package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.object.factory.util.exception.ErrorMessages;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import io.github.gregoryfeijon.serializer.provider.util.gson.GsonTypesUtil;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerAdapter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.getRawType;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isClassMapCollection;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isCollection;

/**
 * Deep copies collections and maps, preserving generic type information
 * and handling element type conversion when source and destination types differ.
 *
 * @author gregory.feijon
 */
@SuppressWarnings("java:S6204")
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
            if (isCollection(sourceValue.getClass())) {
                return cloneCollection((Collection<?>) sourceValue, genericType);
            } else {
                return cloneMap((Map<?, ?>) sourceValue, genericType);
            }
        } catch (Exception ex) {
            throw new ApiException(ErrorMessages.CLONE_COLLECTION_MAP_ERROR, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object cloneCollection(Collection<?> sourceCollection, Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        if (sourceCollection.isEmpty()) {
            return deserializeEmptyCollection(genericType);
        }

        Object firstElement = sourceCollection.iterator().next();
        Class<?> sourceElementType = firstElement.getClass();
        Class<?> targetElementType = TypeResolver.extractCollectionElementType(genericType);

        if (isClassMapCollection(sourceElementType)) {
            return cloneNestedCollection(sourceCollection, genericType, firstElement, targetElementType);
        }

        return cloneSimpleCollection(sourceCollection, genericType, sourceElementType, targetElementType);
    }

    @SuppressWarnings("unchecked")
    private static Object cloneMap(Map<?, ?> sourceMap, Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        if (sourceMap.isEmpty()) {
            return deserializeEmptyMap(genericType);
        }

        Object firstValue = sourceMap.values().iterator().next();
        Class<?> sourceValueType = firstValue.getClass();
        Class<?> targetValueType = TypeResolver.extractMapValueType(genericType);

        if (isClassMapCollection(sourceValueType)) {
            return cloneNestedMap(sourceMap, genericType, firstValue, targetValueType);
        }

        return cloneSimpleMap(sourceMap, genericType, sourceValueType, targetValueType);
    }

    private static Object deserializeEmptyCollection(Type genericType) {
        SerializerAdapter serializer = ObjectCloner.getSerializer();
        String jsonClone = serializer.serialize(Collections.emptyList());
        return serializer.deserialize(jsonClone, genericType);
    }

    private static Object deserializeEmptyMap(Type genericType) {
        SerializerAdapter serializer = ObjectCloner.getSerializer();
        String jsonClone = serializer.serialize(Collections.emptyMap());
        return serializer.deserialize(jsonClone, genericType);
    }

    private static Object cloneNestedCollection(Collection<?> sourceCollection,
                                                 Type genericType,
                                                 Object firstElement,
                                                 Class<?> targetElementType) throws ClassNotFoundException {
        Class<?> innermostSourceType = TypeResolver.getInnermostElementType(firstElement);

        if (innermostSourceType == targetElementType) {
            SerializerAdapter serializer = ObjectCloner.getSerializer();
            String jsonClone = serializer.serialize(sourceCollection);
            return verifyList(sourceCollection, genericType, jsonClone);
        }

        return convertNestedCollectionElements(sourceCollection, genericType);
    }

    private static List<Object> convertNestedCollectionElements(Collection<?> sourceCollection,
                                                                 Type genericType) {
        Type nestedType = TypeResolver.getNestedGenericType(genericType, 0);
        List<Object> convertedList = new ArrayList<>();

        for (Object item : sourceCollection) {
            Object converted = serializingCloneCollectionMap(item, nestedType);
            convertedList.add(converted);
        }

        return convertedList;
    }

    private static Object cloneSimpleCollection(Collection<?> sourceCollection,
                                                 Type genericType,
                                                 Class<?> sourceElementType,
                                                 Class<?> targetElementType)
            throws ClassNotFoundException {
        if (sourceElementType == targetElementType) {
            SerializerAdapter serializer = ObjectCloner.getSerializer();
            String jsonClone = serializer.serialize(sourceCollection);
            return verifyList(sourceCollection, genericType, jsonClone);
        }

        return convertCollectionElements(sourceCollection, targetElementType);
    }

    private static List<Object> convertCollectionElements(Collection<?> sourceCollection,
                                                           Class<?> targetElementType) {
        List<Object> convertedList = new ArrayList<>();

        for (Object item : sourceCollection) {
            Object converted = ObjectFactoryUtil.createFromObject(item, targetElementType);
            convertedList.add(converted);
        }

        return convertedList;
    }

    @SuppressWarnings({"unchecked", "java:S108"})
    private static Object verifyList(Object sourceValue, Type genericType, String jsonClone)
            throws ClassNotFoundException {
        SerializerAdapter serializer = ObjectCloner.getSerializer();

        try {
            TypeResolver.verifyType(genericType);
            return serializer.deserialize(jsonClone, genericType);
        } catch (BeanInstantiationException | ClassNotFoundException ex) {
            List<Object> aux = new ArrayList<>(Collections.checkedCollection((Collection<Object>) sourceValue, Object.class));
            if (!CollectionUtils.isEmpty(aux)) {
                Class<?> objectType = aux.getFirst().getClass();
                Type fallbackType = GsonTypesUtil.getType(getRawType(genericType), objectType);
                return serializer.deserialize(jsonClone, fallbackType);
            }
            return null;
        }
    }

    private static Object cloneNestedMap(Map<?, ?> sourceMap,
                                          Type genericType,
                                          Object firstValue,
                                          Class<?> targetValueType) {
        Class<?> innermostSourceType = TypeResolver.getInnermostElementType(firstValue);

        if (innermostSourceType == targetValueType) {
            SerializerAdapter serializer = ObjectCloner.getSerializer();
            String jsonClone = serializer.serialize(sourceMap);
            return serializer.deserialize(jsonClone, genericType);
        }

        return convertNestedMapValues(sourceMap, genericType);
    }

    private static Map<Object, Object> convertNestedMapValues(Map<?, ?> sourceMap, Type genericType) {
        Type nestedType = TypeResolver.getNestedGenericType(genericType, 1);
        Map<Object, Object> convertedMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object converted = serializingCloneCollectionMap(entry.getValue(), nestedType);
            convertedMap.put(entry.getKey(), converted);
        }

        return convertedMap;
    }

    private static Object cloneSimpleMap(Map<?, ?> sourceMap,
                                          Type genericType,
                                          Class<?> sourceValueType,
                                          Class<?> targetValueType) {
        if (sourceValueType == targetValueType) {
            SerializerAdapter serializer = ObjectCloner.getSerializer();
            String jsonClone = serializer.serialize(sourceMap);
            return serializer.deserialize(jsonClone, genericType);
        }

        return convertMapValues(sourceMap, targetValueType);
    }

    private static Map<Object, Object> convertMapValues(Map<?, ?> sourceMap, Class<?> targetValueType) {
        Map<Object, Object> convertedMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object convertedValue = ObjectFactoryUtil.createFromObject(entry.getValue(), targetValueType);
            convertedMap.put(entry.getKey(), convertedValue);
        }

        return convertedMap;
    }
}
