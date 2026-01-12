package io.github.gregoryfeijon.object.factory.util.utils.serialization;


import io.github.gregoryfeijon.object.factory.commons.utils.FieldUtil;
import io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil;
import io.github.gregoryfeijon.object.factory.commons.utils.ReflectionUtil;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectConstructor;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import io.github.gregoryfeijon.object.factory.util.domain.model.ClassPairKey;
import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.serializer.provider.util.gson.GsonTypesUtil;
import io.github.gregoryfeijon.serializer.provider.util.serialization.SerializationUtil;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerAdapter;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerProvider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.defaultValueFor;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.getRawType;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isClassMapCollection;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isCollection;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isPrimitiveOrEnum;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isWrapperType;
import static java.util.Arrays.stream;

/**
 * Utility class for creating deep copies of objects.
 * <p>
 * This class provides methods to create copies of objects, including complex objects
 * with nested structures, collections, and primitive types. It uses a combination of
 * reflection, serialization, and direct field copying to achieve deep copying.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li>Deep copying with support for nested objects and collections</li>
 *   <li>Hibernate proxy unwrapping to avoid lazy initialization issues</li>
 *   <li>Field-level exclusion via annotations ({@link ObjectCopyExclude}, {@link ObjectCopyExclusions})</li>
 *   <li>Class-level exclusion via {@link ObjectConstructor#exclude()}</li>
 *   <li>Custom field name mapping via {@link FieldCopyName}</li>
 *   <li>Performance optimization through reflection caching</li>
 *   <li>Type conversion support (e.g., enum to string, wrapper to primitive)</li>
 * </ul>
 *
 * @author gregory.feijon
 */
@SuppressWarnings("java:S6204")
//warning for .toList() suppressed, as it does not apply to this utility class
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObjectFactoryUtil {

    private static final SerializerAdapter SERIALIZER;
    private static final Predicate<Field> PREDICATE_MODIFIERS;

    /**
     * Cache storing field mappings for each class, keyed by normalized field name.
     * This cache avoids repeated reflection and annotation lookups.
     * <p>
     * Thread-safe via {@link ConcurrentHashMap}.
     * </p>
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache of resolved source fields to copy per class pair (source, destination).
     * <p>
     * This cache stores the computed list of copyable fields for each unique combination
     * of source and destination classes, significantly improving performance for repeated
     * copy operations.
     * </p>
     */
    private static final Map<ClassPairKey, List<Field>> FIELDS_TO_COPY_CACHE = new ConcurrentHashMap<>();

    static {
        SERIALIZER = SerializerProvider.getAdapter();
        PREDICATE_MODIFIERS = predicateModifiers();
    }

    /**
     * Creates deep copies of all objects in a collection.
     * <p>
     * This method creates a new list containing deep copies of all objects in the provided collection.
     * Each object is copied to a new instance of the same type.
     * </p>
     *
     * @param <T>            the type of objects in the collection
     * @param entitiesToCopy the collection of objects to copy
     * @return a list containing deep copies of the original objects
     * @throws ApiException if the collection is empty or if an error occurs during copying
     */
    public static <T> List<T> copyAllObjectsFromCollection(Collection<T> entitiesToCopy) {
        verifyCollection(entitiesToCopy);
        return entitiesToCopy.stream().map(createCopy()).collect(Collectors.toList());
    }

    /**
     * Creates deep copies of all objects in a collection, converting them to a different type.
     * <p>
     * This method creates a new list containing deep copies of all objects in the provided collection.
     * Each object is converted to the specified return type. The return type does not need to be
     * the same as the source type; fields with matching names will be copied.
     * </p>
     *
     * @param <T>            the type of the resulting collection elements
     * @param entitiesToCopy the collection of objects to copy
     * @param returnType     the class of the target type
     * @return a list containing deep copies of the original objects, converted to the target type
     * @throws ApiException if the collection is empty or if an error occurs during copying
     */
    public static <T> List<T> copyAllObjectsFromCollection(Collection<?> entitiesToCopy, Class<T> returnType) {
        verifyCollection(entitiesToCopy);
        return entitiesToCopy.stream().map(createCopy(returnType)).collect(Collectors.toList());
    }

    /**
     * Creates deep copies of all objects in a collection and returns them in a custom collection type.
     * <p>
     * This method allows you to specify the type of collection to return (e.g., HashSet, LinkedList).
     * The returned collection is populated with deep copies of all objects from the source collection.
     * </p>
     *
     * @param <T>            the type of objects in the collection
     * @param <U>            the type of the resulting collection, created by the supplier
     * @param entitiesToCopy the collection of objects to copy
     * @param supplier       a supplier that creates the target collection
     * @return a collection of the specified type containing deep copies of the original objects
     * @throws ApiException if the collection is empty, supplier is null, or an error occurs during copying
     */
    public static <T, U extends Collection<T>> U copyAllObjectsFromCollection(Collection<T> entitiesToCopy,
                                                                              Supplier<U> supplier) {
        verifyCollectionAndSupplier(entitiesToCopy, supplier);
        return entitiesToCopy.stream().map(createCopy()).collect(Collectors.toCollection(supplier));
    }

    /**
     * Creates deep copies of all objects in a collection, converting them to a different type
     * and returning them in a custom collection type.
     * <p>
     * This method combines type conversion with custom collection type specification. Each object
     * is converted to the target type and added to a collection created by the supplier.
     * </p>
     *
     * @param <T>            the target type for converted objects
     * @param <S>            the source type of objects in the collection
     * @param <U>            the type of the resulting collection
     * @param entitiesToCopy the collection of objects to copy
     * @param supplier       a supplier that creates the target collection
     * @param returnType     the class of the target type
     * @return a collection of the specified type containing deep copies of the original objects
     * @throws ApiException if the collection is empty, supplier is null, or an error occurs during copying
     */
    public static <T, S, U extends Collection<T>> U copyAllObjectsFromCollection(Collection<S> entitiesToCopy,
                                                                                 Supplier<U> supplier, Class<T> returnType) {
        verifyCollectionAndSupplier(entitiesToCopy, supplier);
        return entitiesToCopy.stream().map(createCopy(returnType)).collect(Collectors.toCollection(supplier));
    }

    /**
     * Verifies that both a collection and a supplier are non-null.
     *
     * @param <T>            the type of objects in the collection
     * @param <U>            the type of collection to be created by the supplier
     * @param entitiesToCopy the collection to verify
     * @param supplier       the supplier to verify
     * @throws ApiException if either the collection is empty or the supplier is null
     */
    private static <T, U> void verifyCollectionAndSupplier(Collection<T> entitiesToCopy, Supplier<U> supplier) {
        verifyCollection(entitiesToCopy);
        if (supplier == null) {
            throw new ApiException("The specified collection type for return is null.");
        }
    }

    /**
     * Verifies that a collection is not empty.
     *
     * @param <T>            the type of objects in the collection
     * @param entitiesToCopy the collection to verify
     * @throws ApiException if the collection is empty
     */
    private static <T> void verifyCollection(Collection<T> entitiesToCopy) {
        if (CollectionUtils.isEmpty(entitiesToCopy)) {
            throw new ApiException("The collection to be copied has no elements.");
        }
    }

    /**
     * Creates a function that produces a deep copy of an object.
     *
     * @param <T> the type of objects to copy
     * @return a function that creates deep copies
     */
    private static <T> Function<T, T> createCopy() {
        return ObjectFactoryUtil::createFromObject;
    }

    /**
     * Creates a function that produces a deep copy of an object, converting it to a different type.
     *
     * @param <T>        the target type
     * @param <S>        the source type
     * @param returnType the class of the target type
     * @return a function that creates deep copies with type conversion
     */
    private static <T, S> Function<S, T> createCopy(Class<T> returnType) {
        return i -> createFromObject(i, returnType);
    }

    /**
     * Creates a deep copy of an object, converting it to a different type.
     * <p>
     * This method creates a new instance of the target type and copies
     * all matching fields from the source object to the new instance.
     * </p>
     *
     * @param <T>        the target type
     * @param <S>        the source type
     * @param source     the source object to copy
     * @param returnType the class of the target type
     * @return a new instance of the target type with copied fields
     * @throws ApiException if the source object is null or if an error occurs during copying
     */
    public static <T, S> T createFromObject(S source, Class<T> returnType) {
        verifySourceObject(source);
        T dest = BeanUtils.instantiateClass(returnType);
        createFromObject(source, dest);
        return dest;
    }

    /**
     * Creates a deep copy of an object of the same type.
     * <p>
     * This method creates a new instance of the same class as the source object
     * and copies all fields from the source to the new instance.
     * </p>
     *
     * @param <T>    the type of the object
     * @param source the object to copy
     * @return a deep copy of the source object
     * @throws ApiException if the source object is null or if an error occurs during copying
     */
    @SuppressWarnings("unchecked")
    public static <T> T createFromObject(T source) {
        verifySourceObject(source);
        Object dest = BeanUtils.instantiateClass(source.getClass());
        createFromObject(source, dest);
        return (T) dest;
    }

    /**
     * Copies all fields from a source object to a destination object.
     * <p>
     * This method is the core implementation of the object copying functionality.
     * It handles different types of fields, including primitive types, collections,
     * and nested objects. The copying process:
     * </p>
     * <ul>
     *   <li>Maps corresponding fields between source and destination</li>
     *   <li>Unwraps Hibernate proxies to avoid lazy initialization issues</li>
     *   <li>Performs type conversion when necessary</li>
     *   <li>Recursively copies nested objects and collections</li>
     *   <li>Respects field exclusion annotations</li>
     * </ul>
     *
     * @param <S>    the source type
     * @param <T>    the destination type
     * @param source the source object
     * @param dest   the destination object
     * @throws ApiException if an error occurs during copying
     */
    public static <T, S> void createFromObject(S source, T dest) {
        verifySourceAndDestObjects(source, dest);
        var sourceDestFieldsMap = createSourceDestFieldMaps(source, dest);
        sourceDestFieldsMap.entrySet().parallelStream().forEach(fieldsEntry -> {
            Object sourceValue = verifyValue(fieldsEntry.getKey(), fieldsEntry.getValue(), source);
            FieldUtil.setProtectedFieldValue(fieldsEntry.getValue(), dest, sourceValue);
        });
    }

    /**
     * Creates a mapping between source and destination fields that share
     * the same logical name, considering {@link FieldCopyName} annotations.
     * <p>
     * This method uses caching to minimize reflection overhead and should be
     * reused across multiple object copy operations.
     * </p>
     *
     * @param <S>    the source object type
     * @param <T>    the destination object type
     * @param source the source object whose fields are to be copied
     * @param dest   the destination object that will receive the values
     * @return a map where the key is the source field and the value is the corresponding destination field;
     * returns an empty map if either source or destination has no fields to copy
     */
    private static <S, T> Map<Field, Field> createSourceDestFieldMaps(S source, T dest) {
        var sourceFields = getFieldsToCopy(source, dest);
        var destFields = ReflectionUtil.getFieldsAsCollection(dest, ArrayList::new);

        if (sourceFields.isEmpty() || destFields.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Field> sourceFieldMap = buildFieldKeyMap(sourceFields);
        Map<String, Field> destFieldMap = buildFieldKeyMap(destFields);

        return sourceFieldMap.entrySet().stream()
                .filter(entry -> destFieldMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        entry -> destFieldMap.get(entry.getKey())
                ));
    }

    /**
     * Builds a key-to-field map for a given class, using cached data when available.
     * <p>
     * The cache key is the declaring class of the provided fields. If the class
     * has already been processed, the cached result is returned immediately.
     * Fields from multiple classes (e.g., through inheritance) are grouped by their
     * declaring class before caching.
     * </p>
     *
     * @param fields the list of fields to process
     * @return a map of normalized field names (case-insensitive) to {@link Field} objects;
     * returns an empty map if the list of fields is empty
     */
    private static Map<String, Field> buildFieldKeyMap(List<Field> fields) {
        return fields.stream()
                .collect(Collectors.groupingBy(Field::getDeclaringClass))
                .entrySet().stream()
                .flatMap(entry -> FIELD_KEY_CACHE.computeIfAbsent(
                        entry.getKey(),
                        cls -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        ObjectFactoryUtil::resolveFieldKey,
                                        Function.identity(),
                                        (a, b) -> {
                                            log.warn("Duplicate field key '{}' detected in class '{}'. Keeping first occurrence.",
                                                    a.getName(), cls.getSimpleName());
                                            return a;
                                        })
                                )
                ).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Resolves the logical key name for a field, considering the {@link FieldCopyName} annotation.
     * <p>
     * If the annotation is present and contains a non-empty value, that value is used;
     * otherwise, the field's actual name is used. The result is always normalized to lowercase
     * and trimmed for consistent matching.
     * </p>
     *
     * @param field the field to resolve
     * @return the normalized key representing the logical field name
     */
    private static String resolveFieldKey(Field field) {
        FieldCopyName ann = field.getAnnotation(FieldCopyName.class);
        String key = (ann != null && StringUtils.hasText(ann.value()))
                ? ann.value()
                : field.getName();
        return key.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Verifies that both source and destination objects are non-null.
     *
     * @param <T>    the type of the destination object
     * @param <S>    the type of the source object
     * @param source the source object
     * @param dest   the destination object
     * @throws ApiException if either object is null
     */
    private static <T, S> void verifySourceAndDestObjects(S source, T dest) {
        verifySourceObject(source);
        if (dest == null) {
            throw new ApiException("The destination object is null.");
        }
    }

    /**
     * Verifies that the source object to be copied is non-null.
     *
     * @param <S>    the type of the source object
     * @param source the source object
     * @throws ApiException if the source object is null
     */
    private static <S> void verifySourceObject(S source) {
        if (source == null) {
            throw new ApiException("The object to be copied is null.");
        }
    }

    /**
     * Retrieves all fields that should be copied from the source object.
     * <p>
     * This method applies caching to avoid repeated reflection and annotation lookups.
     * The first time a (source, destination) class pair is processed, it computes the eligible fields
     * and stores them in an internal cache for fast future lookups.
     * </p>
     * <p>
     * Exclusion rules applied:
     * </p>
     * <ul>
     *   <li>Static final fields (constants) are always excluded</li>
     *   <li>Class-level exclusions declared via {@link ObjectConstructor#exclude()} (destination only)
     *       and {@link ObjectCopyExclusions} (both source and destination) are applied</li>
     *   <li>Fields annotated with {@link ObjectCopyExclude} in either source or destination are excluded</li>
     *   <li>Exclusions from superclasses are also considered (inheritance support)</li>
     * </ul>
     *
     * @param <T>    the type of the destination object
     * @param <S>    the type of the source object
     * @param source the source instance to copy fields from
     * @param dest   the destination instance to copy fields to
     * @return a cached or computed {@link List} of {@link Field} objects representing the copyable fields
     */
    private static <T, S> List<Field> getFieldsToCopy(S source, T dest) {
        ClassPairKey cacheKey = new ClassPairKey(source.getClass(), dest.getClass());

        return FIELDS_TO_COPY_CACHE.computeIfAbsent(cacheKey, key -> {
            List<Field> sourceFields = new ArrayList<>(ReflectionUtil.getFieldsAsCollection(source));
            Set<Field> fieldsToRemove = sourceFields.stream()
                    .filter(PREDICATE_MODIFIERS)
                    .collect(Collectors.toSet());

            addAnnotationBasedExclusions(fieldsToRemove, sourceFields, dest, true);
            addAnnotationBasedExclusions(fieldsToRemove, sourceFields, source, false);

            excludeAnnotatedSourceFields(fieldsToRemove, sourceFields);
            excludeAnnotatedDestinationFields(fieldsToRemove, sourceFields, dest);

            if (!fieldsToRemove.isEmpty()) {
                sourceFields.removeAll(fieldsToRemove);
            }

            return List.copyOf(sourceFields);
        });
    }

    /**
     * Adds exclusion rules defined in class-level annotations, supporting inheritance.
     * <p>
     * This method resolves and applies exclusion rules declared on the class level via
     * {@link ObjectConstructor#exclude()} and {@link ObjectCopyExclusions}, including those
     * defined in superclasses.
     * </p>
     * <p>
     * Although exclusions are not cached at this level, they are indirectly cached through
     * the final result stored in {@code FIELDS_TO_COPY_CACHE}, avoiding redundant reflection
     * in subsequent copy operations for the same class pair.
     * </p>
     *
     * @param fieldsToRemove           the {@link Set} of fields to be excluded
     * @param sourceFields             the {@link List} of source fields available for copying
     * @param target                   the target instance (either source or destination object)
     * @param includeObjectConstructor whether to include exclusions from {@link ObjectConstructor#exclude()}
     */
    private static void addAnnotationBasedExclusions(Set<Field> fieldsToRemove,
                                                     List<Field> sourceFields,
                                                     Object target,
                                                     boolean includeObjectConstructor) {
        Set<String> excludeFields = getClassExclusions(target.getClass(), includeObjectConstructor);

        if (!excludeFields.isEmpty()) {
            excludeListedFields(fieldsToRemove, sourceFields, excludeFields.toArray(new String[0]));
        }
    }

    /**
     * Collects exclusion field names declared at the class level via
     * {@link ObjectConstructor#exclude()} or {@link ObjectCopyExclusions}, traversing the
     * superclass hierarchy.
     * <p>
     * Exclusions resolved by this method are not individually cached. However, once
     * the final copyable field list is computed and stored in {@code FIELDS_TO_COPY_CACHE},
     * this logic will not be re-executed for the same class combination.
     * </p>
     *
     * @param clazz                    the target class to inspect
     * @param includeObjectConstructor whether to include {@link ObjectConstructor#exclude()} values
     * @return an immutable {@link Set} of field names to exclude
     */
    private static Set<String> getClassExclusions(Class<?> clazz, boolean includeObjectConstructor) {
        Set<String> exclusions = new HashSet<>();

        while (clazz != null && clazz != Object.class) {
            if (includeObjectConstructor && clazz.isAnnotationPresent(ObjectConstructor.class)) {
                exclusions.addAll(Arrays.asList(clazz.getAnnotation(ObjectConstructor.class).exclude()));
            }
            if (clazz.isAnnotationPresent(ObjectCopyExclusions.class)) {
                exclusions.addAll(Arrays.asList(clazz.getAnnotation(ObjectCopyExclusions.class).value()));
            }
            clazz = clazz.getSuperclass();
        }

        return Set.copyOf(exclusions);
    }

    /**
     * Excludes all fields from the source object that are annotated with {@link ObjectCopyExclude}.
     *
     * @param fieldsToRemove the {@link Set} of fields to be excluded
     * @param sourceFields   the {@link List} of fields declared in the source object
     */
    private static void excludeAnnotatedSourceFields(Set<Field> fieldsToRemove, List<Field> sourceFields) {
        sourceFields.stream()
                .filter(f -> f.isAnnotationPresent(ObjectCopyExclude.class))
                .forEach(fieldsToRemove::add);
    }

    /**
     * Excludes source fields whose names match destination fields annotated with {@link ObjectCopyExclude}.
     * <p>
     * This method creates a map of source fields for efficient lookup, then checks each destination field
     * for the {@link ObjectCopyExclude} annotation. If found, the corresponding source field (by name)
     * is added to the exclusion set.
     * </p>
     *
     * @param <T>            the type of the destination object
     * @param fieldsToRemove the {@link Set} of fields to be excluded
     * @param sourceFields   the {@link List} of fields declared in the source object
     * @param dest           the destination instance to inspect for {@link ObjectCopyExclude} annotations
     */
    private static <T> void excludeAnnotatedDestinationFields(Set<Field> fieldsToRemove,
                                                              List<Field> sourceFields,
                                                              T dest) {
        List<Field> destFields = ReflectionUtil.getFieldsAsCollection(dest, ArrayList::new);

        Map<String, Field> sourceFieldMap = sourceFields.stream()
                .collect(Collectors.toMap(
                        ObjectFactoryUtil::resolveFieldKey,
                        Function.identity(),
                        (a, b) -> a
                ));

        destFields.stream()
                .filter(f -> f.isAnnotationPresent(ObjectCopyExclude.class))
                .map(ObjectFactoryUtil::resolveFieldKey)
                .map(sourceFieldMap::get)
                .filter(Objects::nonNull)
                .forEach(fieldsToRemove::add);
    }

    /**
     * Marks for exclusion all source fields whose names match any of the given exclusion field names.
     *
     * @param fieldsToRemove the {@link Set} of fields to be excluded
     * @param sourceFields   the {@link List} of source fields available for copying
     * @param exclude        the array of field names declared for exclusion
     */
    private static void excludeListedFields(Set<Field> fieldsToRemove,
                                            List<Field> sourceFields,
                                            String[] exclude) {
        stream(exclude)
                .forEach(excludeField -> sourceFields.stream()
                        .filter(sourceField -> resolveFieldKey(sourceField).equalsIgnoreCase(excludeField))
                        .findAny()
                        .ifPresentOrElse(fieldsToRemove::add,
                                () -> log.trace("ObjectCopyExclusions: field '{}' not found in source class. Skipping" +
                                        " exclusion.", excludeField)
                        )
                );
    }

    /**
     * Verifies and processes the value to be copied, handling special cases where source and
     * destination field types differ.
     * <p>
     * This method handles several type conversion scenarios:
     * </p>
     * <ul>
     *   <li>Wrapper to primitive conversions (and vice versa)</li>
     *   <li>Enum to String conversions (and vice versa)</li>
     *   <li>Enum to Enum conversions (matching by name)</li>
     *   <li>Collection and Map types (returns null for manual handling)</li>
     *   <li>Hibernate proxy unwrapping before copying</li>
     * </ul>
     *
     * @param <S>         the type of the source object
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param source      the source object instance
     * @return the processed value ready to be set in the destination field
     */
    private static <S> Object verifyValue(Field sourceField, Field destField, S source) {
        Object sourceValue = FieldUtil.getProtectedFieldValue(sourceField, source);
        sourceValue = unproxyValueIfNeeded(sourceValue);
        Class<?> sourceFieldType = sourceField.getType();
        Class<?> destFieldType = destField.getType();

        if (sourceFieldType == destFieldType) {
            return copyValue(sourceField, destField, sourceValue);
        }

        if (isWrapperType(sourceFieldType) && destFieldType.isPrimitive() && sourceValue == null) {
            return defaultValueFor(destFieldType);
        }

        if (isWrapperType(destFieldType) && sourceFieldType.isPrimitive() && Objects.equals(sourceValue, defaultValueFor(sourceFieldType))) {
            return null;
        }

        if (sourceFieldType.isEnum() || destFieldType.isEnum()) {
            return validateEnums(sourceField, destField, sourceValue);
        }

        if (isClassMapCollection(destFieldType) || isClassMapCollection(sourceFieldType)) {
            return null;
        }

        return copyValue(sourceField, destField, sourceValue);
    }

    /**
     * Removes Hibernate proxy wrappers from an object, if present.
     * <p>
     * This method recursively unwraps Hibernate proxies from objects, collections, and maps.
     * For collections and maps, it only creates new instances if proxies are actually found,
     * preserving the original instance otherwise to avoid unnecessary object creation.
     * </p>
     *
     * @param value the object to unproxy
     * @return the unproxied object, or the original if no proxy was found
     */
    private static Object unproxyValueIfNeeded(Object value) {
        if (HibernateProxyChecker.isHibernateProxy(value)) {
            return HibernateProxyChecker.unproxy(value);
        }

        if (value instanceof Collection<?> collection) {
            return unproxyCollection(collection);
        }

        if (value instanceof Map<?, ?> map) {
            return unproxyMap(map);
        }

        return value;
    }

    /**
     * Unproxies all elements in a collection, preserving the original collection type.
     * <p>
     * If no proxies are found, returns the original collection unchanged to avoid
     * unnecessary object creation. If proxies are found, creates a new collection of
     * the same type (or a compatible fallback) with all elements unproxied.
     * </p>
     *
     * @param collection the collection to unproxy
     * @return the unproxied collection, or the original if no proxies were found
     */
    private static Collection<?> unproxyCollection(Collection<?> collection) {
        if (!containsProxy(collection)) {
            return collection;
        }

        List<?> unproxied = collection.stream()
                .map(ObjectFactoryUtil::unproxyValueIfNeeded)
                .toList();

        return recreateCollection(collection, unproxied);
    }

    /**
     * Unproxies all keys and values in a map, preserving the original map type.
     * <p>
     * If no proxies are found, returns the original map unchanged to avoid
     * unnecessary object creation. If proxies are found, creates a new map of
     * the same type (or a compatible fallback) with all keys and values unproxied.
     * </p>
     *
     * @param map the map to unproxy
     * @return the unproxied map, or the original if no proxies were found
     */
    private static Map<?, ?> unproxyMap(Map<?, ?> map) {
        if (!containsProxy(map)) {
            return map;
        }

        Map<?, ?> unproxied = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> unproxyValueIfNeeded(e.getKey()),
                        e -> unproxyValueIfNeeded(e.getValue())
                ));

        return recreateMap(map, unproxied);
    }

    /**
     * Checks if a collection contains any Hibernate proxy instances.
     *
     * @param collection the collection to check
     * @return {@code true} if at least one element is a Hibernate proxy, {@code false} otherwise
     */
    private static boolean containsProxy(Collection<?> collection) {
        return collection.stream()
                .anyMatch(HibernateProxyChecker::isHibernateProxy);
    }

    /**
     * Checks if a map contains any Hibernate proxy instances in its keys or values.
     *
     * @param map the map to check
     * @return {@code true} if at least one key or value is a Hibernate proxy, {@code false} otherwise
     */
    private static boolean containsProxy(Map<?, ?> map) {
        return map.entrySet().stream()
                .anyMatch(e -> HibernateProxyChecker.isHibernateProxy(e.getKey())
                        || HibernateProxyChecker.isHibernateProxy(e.getValue()));
    }

    /**
     * Attempts to recreate a collection of the same type as the original,
     * falling back to common implementations if the exact type cannot be instantiated.
     * <p>
     * This method first tries to instantiate the exact type of the original collection
     * using its default constructor. If that fails (e.g., for immutable collections or
     * collections without a no-arg constructor), it falls back to standard implementations
     * based on the collection's interface type.
     * </p>
     *
     * @param original  the original collection (used to determine the target type)
     * @param unproxied the unproxied elements to populate the new collection
     * @return a new collection of the same type (or a compatible fallback) containing the unproxied elements
     */
    @SuppressWarnings("unchecked")
    private static Collection<?> recreateCollection(Collection<?> original, List<?> unproxied) {
        try {
            Collection<Object> result = original.getClass()
                    .getDeclaredConstructor()
                    .newInstance();
            result.addAll(unproxied);
            return result;
        } catch (Exception e) {
            log.trace("Could not instantiate exact collection type {}. Using type-based fallback.",
                    original.getClass().getSimpleName());
        }

        return switch (original) {
            case Set<?> ignored -> new HashSet<>(unproxied);
            case Deque<?> ignored -> new ArrayDeque<>(unproxied);
            case Queue<?> ignored -> new LinkedList<>(unproxied);
            default -> new ArrayList<>(unproxied);
        };
    }

    /**
     * Attempts to recreate a map of the same type as the original,
     * falling back to HashMap if the exact type cannot be instantiated.
     * <p>
     * This method first tries to instantiate the exact type of the original map
     * using its default constructor. If that fails (e.g., for immutable maps or
     * maps without a no-arg constructor), it falls back to standard implementations
     * based on the map's concrete type.
     * </p>
     *
     * @param original  the original map (used to determine the target type)
     * @param unproxied the unproxied entries to populate the new map
     * @return a new map of the same type (or HashMap as fallback) containing the unproxied entries
     */
    @SuppressWarnings("unchecked")
    private static Map<?, ?> recreateMap(Map<?, ?> original, Map<?, ?> unproxied) {
        try {
            Map<Object, Object> result = original.getClass()
                    .getDeclaredConstructor()
                    .newInstance();
            result.putAll(unproxied);
            return result;
        } catch (Exception e) {
            log.trace("Could not instantiate exact map type {}. Using type-based fallback.",
                    original.getClass().getSimpleName());
        }

        return switch (original) {
            case LinkedHashMap<?, ?> ignored -> new LinkedHashMap<>(unproxied);
            case TreeMap<?, ?> ignored -> new TreeMap<>(unproxied);
            case ConcurrentHashMap<?, ?> ignored -> new ConcurrentHashMap<>(unproxied);
            default -> new HashMap<>(unproxied);
        };
    }

    /**
     * Validates and converts enum values between source and destination fields.
     * <p>
     * This method handles several enum conversion scenarios:
     * </p>
     * <ul>
     *   <li>String to Enum: Finds the enum constant matching the string value</li>
     *   <li>Enum to String: Converts the enum to its string representation</li>
     *   <li>Enum to Enum: Converts between different enum types by matching names</li>
     * </ul>
     *
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param sourceValue the value to convert
     * @return the converted value, or null if conversion is not possible
     */
    private static Object validateEnums(Field sourceField, Field destField, Object sourceValue) {
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
     * <p>
     * This method searches through all constants of the specified enum type
     * and returns the first one whose toString() value matches the source value.
     * </p>
     *
     * @param type        the enum class to search
     * @param sourceValue the value to match (compared using toString())
     * @return the matching enum constant, or null if no match is found
     */
    private static Object findEnumConstantEquivalent(Class<?> type, Object sourceValue) {
        return Stream.of(type.getEnumConstants())
                .filter(enumConstant -> Objects.equals(enumConstant.toString(), sourceValue))
                .findFirst()
                .orElse(null);
    }

    // ============================================================================
// MAIN COPY METHODS
// ============================================================================

    /**
     * Determines the appropriate copying strategy based on the field types and copies the value.
     * <p>
     * This method selects the copying strategy based on the type of the source field:
     * </p>
     * <ul>
     *   <li>Primitives and Enums: Direct value assignment</li>
     *   <li>Wrapper types: Serialization-based cloning</li>
     *   <li>Collections and Maps: Serialization with generic type preservation</li>
     *   <li>Complex objects: Deep copy via serialization</li>
     * </ul>
     *
     * @param sourceField the field in the source object
     * @param destField   the corresponding field in the destination object
     * @param sourceValue the value to copy
     * @return the copied value
     */
    private static Object copyValue(Field sourceField, Field destField, Object sourceValue) {
        Class<?> sourceFieldType = sourceField.getType();
        Class<?> destFieldType = destField.getType();

        if (isPrimitiveOrEnum(sourceFieldType)) {
            return sourceValue;
        }
        if (isWrapperType(sourceFieldType)) {
            return serializingClone(sourceValue, destFieldType);
        }
        if (isClassMapCollection(sourceField.getType())) {
            return serializingCloneCollectionMap(sourceValue, destField.getGenericType());
        }
        try {
            return serializingCloneObjects(sourceValue, destFieldType);
        } catch (Exception ex) {
            throw new ApiException(ex.getMessage());
        }
    }

    /**
     * Creates a serialized clone of a wrapper type value.
     * <p>
     * This method handles null values and delegates to {@link #serializingCloneObjects(Object, Class)}
     * for actual cloning.
     * </p>
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value, or null if the source value is null
     */
    private static Object serializingClone(Object sourceValue, Class<?> clazz) {
        return sourceValue != null ? serializingCloneObjects(sourceValue, clazz) : null;
    }

    /**
     * Creates a deep copy of an object via serialization.
     * <p>
     * This method uses different serialization strategies based on the relationship
     * between the source value's type and the target class:
     * </p>
     * <ul>
     *   <li><b>Target is simple type:</b> Uses Java binary serialization</li>
     *   <li><b>Source is wrapper type:</b> Uses Java binary serialization to create new reference</li>
     *   <li><b>Same type (deep clone):</b> Uses JSON serialization</li>
     *   <li><b>Different types (conversion):</b> Uses recursive {@link #createFromObject}</li>
     * </ul>
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value
     */
    private static Object serializingCloneObjects(Object sourceValue, Class<?> clazz) {
        if (sourceValue == null) {
            return null;
        }
        Class<?> sourceClass = sourceValue.getClass();

        if (ReflectionTypeUtil.isSimpleType(clazz)) {
            return cloneToBinaryFormat(sourceValue);
        }

        if (ReflectionTypeUtil.isWrapperType(sourceClass)) {
            return cloneToBinaryFormat(sourceValue);
        }

        if (sourceClass == clazz) {
            return cloneToJsonFormat(sourceValue, clazz);
        }

        return createFromObject(sourceValue, clazz);
    }

    /**
     * Clones an object using Java binary serialization.
     * <p>
     * Creates a new reference in memory by serializing to byte array and deserializing back.
     * Used for simple types, wrappers, and primitives.
     * </p>
     *
     * @param sourceValue the value to clone
     * @return a deep copy of the source value
     */
    private static Object cloneToBinaryFormat(Object sourceValue) {
        byte[] byteClone = SerializationUtils.serialize(sourceValue);
        return SerializationUtil.deserialize(byteClone);
    }

    /**
     * Clones an object using JSON serialization.
     * <p>
     * Creates a deep copy by converting to JSON and back. Used when source and
     * destination types are the same, ensuring all nested references are new.
     * </p>
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value
     */
    private static Object cloneToJsonFormat(Object sourceValue, Class<?> clazz) {
        String jsonClone = SERIALIZER.serialize(sourceValue);
        return SERIALIZER.deserialize(jsonClone, clazz);
    }

    /**
     * Creates a deep copy of a collection or map via serialization, preserving generic type information.
     * <p>
     * This method handles the complexity of copying generic collections and maps by:
     * </p>
     * <ul>
     *   <li>Preserving type parameters during serialization and deserialization</li>
     *   <li>Detecting when source and destination element types differ</li>
     *   <li>Using efficient JSON serialization for same-type deep cloning</li>
     *   <li>Converting elements individually when types differ to respect {@link FieldCopyName} mappings</li>
     *   <li>Respecting field exclusion annotations on nested elements</li>
     * </ul>
     *
     * @param sourceValue the collection or map to clone
     * @param genericType the generic type information of the target field
     * @return a deep copy of the source value, or null if the source value is null
     */
    private static Object serializingCloneCollectionMap(Object sourceValue, Type genericType) {
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
            throw new ApiException("Error cloning collection/map during object copy.", ex);
        }
    }

    /**
     * Clones a collection, handling element type conversion when needed.
     * <p>
     * Delegates to specialized handlers based on collection structure and element types.
     * </p>
     *
     * @param sourceCollection the collection to clone
     * @param genericType      the target generic type
     * @return a new collection with cloned/converted elements
     * @throws ClassNotFoundException     if type resolution fails
     * @throws BeanInstantiationException if type instantiation fails
     */
    @SuppressWarnings("unchecked")
    private static Object cloneCollection(Collection<?> sourceCollection, Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        if (sourceCollection.isEmpty()) {
            return deserializeEmptyCollection(genericType);
        }

        Object firstElement = sourceCollection.iterator().next();
        Class<?> sourceElementType = firstElement.getClass();
        Class<?> targetElementType = extractCollectionElementType(genericType);

        if (isClassMapCollection(sourceElementType)) {
            return cloneNestedCollection(sourceCollection, genericType, firstElement, targetElementType);
        }

        return cloneSimpleCollection(sourceCollection, genericType, sourceElementType, targetElementType);
    }

    /**
     * Clones a map, handling value type conversion when needed.
     * <p>
     * Delegates to specialized handlers based on map structure and value types.
     * </p>
     *
     * @param sourceMap   the map to clone
     * @param genericType the target generic type
     * @return a new map with cloned/converted values
     * @throws ClassNotFoundException     if type resolution fails
     * @throws BeanInstantiationException if type instantiation fails
     */
    @SuppressWarnings("unchecked")
    private static Object cloneMap(Map<?, ?> sourceMap, Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        if (sourceMap.isEmpty()) {
            return deserializeEmptyMap(genericType);
        }

        Object firstValue = sourceMap.values().iterator().next();
        Class<?> sourceValueType = firstValue.getClass();
        Class<?> targetValueType = extractMapValueType(genericType);

        if (isClassMapCollection(sourceValueType)) {
            return cloneNestedMap(sourceMap, genericType, firstValue, targetValueType);
        }

        return cloneSimpleMap(sourceMap, genericType, sourceValueType, targetValueType);
    }

    /**
     * Deserializes an empty collection to the target generic type.
     */
    private static Object deserializeEmptyCollection(Type genericType) {
        String jsonClone = SERIALIZER.serialize(Collections.emptyList());
        return SERIALIZER.deserialize(jsonClone, genericType);
    }

    /**
     * Clones a nested collection structure (e.g., List&lt;List&lt;...&gt;&gt;).
     * <p>
     * Determines if the innermost element types match to decide between
     * JSON serialization (same type) or recursive conversion (different types).
     * </p>
     */
    private static Object cloneNestedCollection(Collection<?> sourceCollection,
                                                Type genericType,
                                                Object firstElement,
                                                Class<?> targetElementType) throws ClassNotFoundException {
        Class<?> innermostSourceType = getInnermostElementType(firstElement);

        if (innermostSourceType == targetElementType) {
            String jsonClone = SERIALIZER.serialize(sourceCollection);
            return verifyList(sourceCollection, genericType, jsonClone);
        }

        return convertNestedCollectionElements(sourceCollection, genericType);
    }

    /**
     * Converts each element of a nested collection recursively.
     */
    private static List<Object> convertNestedCollectionElements(Collection<?> sourceCollection,
                                                                Type genericType) {
        Type nestedType = getNestedGenericType(genericType, 0);
        List<Object> convertedList = new ArrayList<>();

        for (Object item : sourceCollection) {
            Object converted = serializingCloneCollectionMap(item, nestedType);
            convertedList.add(converted);
        }

        return convertedList;
    }

    /**
     * Clones a collection with simple (non-nested) elements.
     */
    private static Object cloneSimpleCollection(Collection<?> sourceCollection,
                                                Type genericType,
                                                Class<?> sourceElementType,
                                                Class<?> targetElementType)
            throws ClassNotFoundException {
        if (sourceElementType == targetElementType) {
            String jsonClone = SERIALIZER.serialize(sourceCollection);
            return verifyList(sourceCollection, genericType, jsonClone);
        }

        return convertCollectionElements(sourceCollection, targetElementType);
    }

    /**
     * Converts each element of a collection to the target type.
     */
    private static List<Object> convertCollectionElements(Collection<?> sourceCollection,
                                                          Class<?> targetElementType) {
        List<Object> convertedList = new ArrayList<>();

        for (Object item : sourceCollection) {
            Object converted = createFromObject(item, targetElementType);
            convertedList.add(converted);
        }

        return convertedList;
    }

    /**
     * Verifies and deserializes a list, handling complex generic type scenarios and nested collections.
     * <p>
     * This method first attempts to deserialize using the provided generic type information.
     * If that fails (typically due to complex nested types like List&lt;List&lt;String&gt;&gt;),
     * it inspects the actual runtime type of the collection elements and uses that information
     * for deserialization. This enables proper handling of deeply nested collection structures.
     * </p>
     *
     * @param sourceValue the collection to deserialize
     * @param genericType the generic type information
     * @param jsonClone   the JSON string representation of the collection
     * @return the deserialized collection
     * @throws ClassNotFoundException if a class required for deserialization cannot be found
     */
    @SuppressWarnings("unchecked")
    private static Object verifyList(Object sourceValue, Type genericType, String jsonClone)
            throws ClassNotFoundException {

        try {
            verifyType(genericType);
            return SERIALIZER.deserialize(jsonClone, genericType);
        } catch (BeanInstantiationException | ClassNotFoundException ex) {
            List<Object> aux = new ArrayList<>(Collections.checkedCollection((Collection<Object>) sourceValue, Object.class));
            if (!CollectionUtils.isEmpty(aux)) {
                Class<?> objectType = aux.getFirst().getClass();
                Type fallbackType = GsonTypesUtil.getType(getRawType(genericType), objectType);
                return SERIALIZER.deserialize(jsonClone, fallbackType);
            }
            return null;
        }
    }

    /**
     * Deserializes an empty map to the target generic type.
     */
    private static Object deserializeEmptyMap(Type genericType) {
        String jsonClone = SERIALIZER.serialize(Collections.emptyMap());
        return SERIALIZER.deserialize(jsonClone, genericType);
    }

    /**
     * Clones a nested map structure (e.g., Map&lt;K, Map&lt;...&gt;&gt;).
     * <p>
     * Determines if the innermost element types match to decide between
     * JSON serialization (same type) or recursive conversion (different types).
     * </p>
     */
    private static Object cloneNestedMap(Map<?, ?> sourceMap,
                                         Type genericType,
                                         Object firstValue,
                                         Class<?> targetValueType) {
        Class<?> innermostSourceType = getInnermostElementType(firstValue);

        if (innermostSourceType == targetValueType) {
            String jsonClone = SERIALIZER.serialize(sourceMap);
            return SERIALIZER.deserialize(jsonClone, genericType);
        }

        return convertNestedMapValues(sourceMap, genericType);
    }

    /**
     * Converts each value of a nested map recursively.
     */
    private static Map<Object, Object> convertNestedMapValues(Map<?, ?> sourceMap, Type genericType) {
        Type nestedType = getNestedGenericType(genericType, 1);
        Map<Object, Object> convertedMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object converted = serializingCloneCollectionMap(entry.getValue(), nestedType);
            convertedMap.put(entry.getKey(), converted);
        }

        return convertedMap;
    }

    /**
     * Clones a map with simple (non-nested) values.
     */
    private static Object cloneSimpleMap(Map<?, ?> sourceMap,
                                         Type genericType,
                                         Class<?> sourceValueType,
                                         Class<?> targetValueType) {
        if (sourceValueType == targetValueType) {
            String jsonClone = SERIALIZER.serialize(sourceMap);
            return SERIALIZER.deserialize(jsonClone, genericType);
        }

        return convertMapValues(sourceMap, targetValueType);
    }

    /**
     * Converts each value of a map to the target type.
     */
    private static Map<Object, Object> convertMapValues(Map<?, ?> sourceMap, Class<?> targetValueType) {
        Map<Object, Object> convertedMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object convertedValue = createFromObject(entry.getValue(), targetValueType);
            convertedMap.put(entry.getKey(), convertedValue);
        }

        return convertedMap;
    }

    /**
     * Verifies that the generic type parameters of a parameterized type are instantiable
     * and returns them as an array of Class objects.
     * <p>
     * This method checks each type parameter to ensure it can be instantiated.
     * Primitives, enums, and wrapper types are considered valid. For other types,
     * an attempt is made to instantiate them to verify they are concrete classes
     * with accessible constructors.
     * </p>
     * <p>
     * For nested generic types (e.g., {@code List<List<String>>}), this method extracts
     * the innermost element type by recursively unwrapping {@link ParameterizedType} instances.
     * </p>
     *
     * @param genericType the parameterized type to verify
     * @return an array of {@link Class} objects representing the type parameters
     * @throws ClassNotFoundException     if a type parameter class cannot be found
     * @throws BeanInstantiationException if a type parameter cannot be instantiated
     */
    private static Class<?>[] verifyAndExtractTypes(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        ParameterizedType paramType = (ParameterizedType) genericType;
        Type[] typeArgs = paramType.getActualTypeArguments();
        Class<?>[] classes = new Class<?>[typeArgs.length];

        for (int i = 0; i < typeArgs.length; i++) {
            Class<?> clazz = extractElementClass(typeArgs[i]);
            if (!isPrimitiveOrEnum(clazz) && !isWrapperType(clazz)) {
                BeanUtils.instantiateClass(clazz);
            }
            classes[i] = clazz;
        }

        return classes;
    }

    /**
     * Verifies that the generic type parameters of a collection are instantiable.
     * <p>
     * Note: This method delegates to {@link #verifyAndExtractTypes(Type)} for
     * consistency and code reuse. The extracted types are validated but not returned.
     * </p>
     *
     * @param genericType the generic type to verify
     * @throws ClassNotFoundException     if a type cannot be found
     * @throws BeanInstantiationException if a type cannot be instantiated
     */
    private static void verifyType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        verifyAndExtractTypes(genericType);
    }

    /**
     * Extracts the innermost element class from a type, handling nested generic types.
     * <p>
     * This method recursively unwraps {@link ParameterizedType} instances to find the
     * actual element class. For example:
     * </p>
     * <ul>
     *   <li>{@code String} → {@code String.class}</li>
     *   <li>{@code List<String>} → {@code String.class}</li>
     *   <li>{@code List<List<String>>} → {@code String.class}</li>
     *   <li>{@code List<List<List<PrimitiveBar>>>} → {@code PrimitiveBar.class}</li>
     * </ul>
     *
     * @param type the type to extract from
     * @return the innermost element class
     * @throws ClassNotFoundException if the class cannot be found
     */
    private static Class<?> extractElementClass(Type type) throws ClassNotFoundException {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                return extractElementClass(typeArgs[0]);
            }
            return ClassUtils.getClass(paramType.getRawType().getTypeName());
        }
        return ClassUtils.getClass(type.getTypeName());
    }

    /**
     * Extracts the element type from a collection generic type.
     * <p>
     * For {@code List<T>}, returns {@code T.class}.
     * </p>
     */
    private static Class<?> extractCollectionElementType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        Class<?>[] types = verifyAndExtractTypes(genericType);
        return types[0];
    }

    /**
     * Extracts the value type from a map generic type.
     * <p>
     * For {@code Map<K, V>}, returns {@code V.class}.
     * </p>
     */
    private static Class<?> extractMapValueType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        Class<?>[] types = verifyAndExtractTypes(genericType);
        return types[1];
    }

    /**
     * Obtém o tipo do elemento mais interno de uma estrutura aninhada.
     * <p>
     * Para estruturas como List&lt;List&lt;List&lt;PrimitiveFoo&gt;&gt;&gt;, retorna PrimitiveFoo.class.
     * </p>
     *
     * @param element o elemento a ser inspecionado
     * @return a classe do elemento mais interno
     */
    private static Class<?> getInnermostElementType(Object element) {
        return switch (element) {
            case null -> Object.class;
            case Collection<?> collection -> collection.isEmpty()
                    ? Object.class
                    : getInnermostElementType(collection.iterator().next());
            case Map<?, ?> map -> map.isEmpty()
                    ? Object.class
                    : getInnermostElementType(map.values().iterator().next());
            default -> element.getClass();
        };
    }

    /**
     * Extrai o tipo genérico aninhado de um tipo parametrizado no índice especificado.
     * <p>
     * Para List&lt;T&gt;, índice 0 retorna T.<br>
     * Para Map&lt;K, V&gt;, índice 0 retorna K e índice 1 retorna V.
     * </p>
     *
     * @param genericType o tipo genérico original
     * @param index       o índice do argumento de tipo (0 para primeiro, 1 para segundo, etc)
     * @return o tipo genérico no índice especificado
     */
    private static Type getNestedGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > index) {
                return typeArgs[index];
            }
        }
        return Object.class;
    }

    /**
     * Creates a predicate that identifies constant fields.
     * <p>
     * The predicate returns true for fields that are both static and final,
     * which typically represent constants that should not be copied.
     * </p>
     *
     * @return a predicate for identifying constant fields
     */
    private static Predicate<Field> predicateModifiers() {
        return p -> Modifier.isStatic(p.getModifiers()) && Modifier.isFinal(p.getModifiers());
    }
}