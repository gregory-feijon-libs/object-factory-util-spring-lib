package io.github.gregoryfeijon.object.factory.util.utils.serialization;


import io.github.gregoryfeijon.object.factory.commons.utils.FieldUtil;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.CopyOrchestrator;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.FieldResolver;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.ValidationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 *   <li>Field-level exclusion via annotations</li>
 *   <li>Custom field name mapping via {@code @FieldCopyName}</li>
 *   <li>Performance optimization through reflection caching</li>
 *   <li>Type conversion support (e.g., enum to string, wrapper to primitive)</li>
 * </ul>
 * <p>
 * This class acts as a facade, delegating internal logic to specialized classes
 * in the {@code internal} package.
 * </p>
 *
 * @author gregory.feijon
 */
@SuppressWarnings("java:S6204")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObjectFactoryUtil {

    /**
     * Minimum number of fields required to use parallel stream processing.
     * <p>
     * For objects with fewer fields than this threshold, sequential processing
     * is used to avoid the overhead of the ForkJoinPool.
     * </p>
     */
    private static final int PARALLEL_STREAM_THRESHOLD = 10;

    /**
     * Creates deep copies of all objects in a collection.
     *
     * @param <T>            the type of objects in the collection
     * @param entitiesToCopy the collection of objects to copy
     * @return a list containing deep copies of the original objects
     */
    public static <T> List<T> copyAllObjectsFromCollection(Collection<T> entitiesToCopy) {
        ValidationUtil.verifyCollection(entitiesToCopy);
        return entitiesToCopy.stream().map(createCopy()).collect(Collectors.toList());
    }

    /**
     * Creates deep copies of all objects in a collection, converting them to a different type.
     *
     * @param <T>            the type of the resulting collection elements
     * @param entitiesToCopy the collection of objects to copy
     * @param returnType     the class of the target type
     * @return a list containing deep copies converted to the target type
     */
    public static <T> List<T> copyAllObjectsFromCollection(Collection<?> entitiesToCopy, Class<T> returnType) {
        ValidationUtil.verifyCollection(entitiesToCopy);
        return entitiesToCopy.stream().map(createCopy(returnType)).collect(Collectors.toList());
    }

    /**
     * Creates deep copies of all objects in a collection and returns them in a custom collection type.
     *
     * @param <T>            the type of objects in the collection
     * @param <U>            the type of the resulting collection
     * @param entitiesToCopy the collection of objects to copy
     * @param supplier       a supplier that creates the target collection
     * @return a collection of the specified type containing deep copies
     */
    public static <T, U extends Collection<T>> U copyAllObjectsFromCollection(Collection<T> entitiesToCopy,
                                                                              Supplier<U> supplier) {
        ValidationUtil.verifyCollectionAndSupplier(entitiesToCopy, supplier);
        return entitiesToCopy.stream().map(createCopy()).collect(Collectors.toCollection(supplier));
    }

    /**
     * Creates deep copies of all objects in a collection, converting them to a different type
     * and returning them in a custom collection type.
     *
     * @param <T>            the target type for converted objects
     * @param <S>            the source type of objects in the collection
     * @param <U>            the type of the resulting collection
     * @param entitiesToCopy the collection of objects to copy
     * @param supplier       a supplier that creates the target collection
     * @param returnType     the class of the target type
     * @return a collection of the specified type containing deep copies
     */
    public static <T, S, U extends Collection<T>> U copyAllObjectsFromCollection(Collection<S> entitiesToCopy,
                                                                                 Supplier<U> supplier, Class<T> returnType) {
        ValidationUtil.verifyCollectionAndSupplier(entitiesToCopy, supplier);
        return entitiesToCopy.stream().map(createCopy(returnType)).collect(Collectors.toCollection(supplier));
    }

    /**
     * Creates a deep copy of an object, converting it to a different type.
     *
     * @param <T>        the target type
     * @param <S>        the source type
     * @param source     the source object to copy
     * @param returnType the class of the target type
     * @return a new instance of the target type with copied fields
     */
    public static <T, S> T createFromObject(S source, Class<T> returnType) {
        ValidationUtil.verifySourceObject(source);
        T dest = BeanUtils.instantiateClass(returnType);
        createFromObject(source, dest);
        return dest;
    }

    /**
     * Creates a deep copy of an object of the same type.
     *
     * @param <T>    the type of the object
     * @param source the object to copy
     * @return a deep copy of the source object
     */
    @SuppressWarnings("unchecked")
    public static <T> T createFromObject(T source) {
        ValidationUtil.verifySourceObject(source);
        Object dest = BeanUtils.instantiateClass(source.getClass());
        createFromObject(source, dest);
        return (T) dest;
    }

    /**
     * Copies all fields from a source object to a destination object.
     *
     * @param <S>    the source type
     * @param <T>    the destination type
     * @param source the source object
     * @param dest   the destination object
     */
    public static <T, S> void createFromObject(S source, T dest) {
        ValidationUtil.verifySourceAndDestObjects(source, dest);
        Map<Field, Field> sourceDestFieldsMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        Stream<Map.Entry<Field, Field>> stream = sourceDestFieldsMap.size() >= PARALLEL_STREAM_THRESHOLD
                ? sourceDestFieldsMap.entrySet().parallelStream()
                : sourceDestFieldsMap.entrySet().stream();

        stream.forEach(fieldsEntry -> {
            Object sourceValue = CopyOrchestrator.verifyValue(fieldsEntry.getKey(), fieldsEntry.getValue(), source);
            FieldUtil.setProtectedFieldValue(fieldsEntry.getValue(), dest, sourceValue);
        });
    }

    private static <T> Function<T, T> createCopy() {
        return ObjectFactoryUtil::createFromObject;
    }

    private static <T, S> Function<S, T> createCopy(Class<T> returnType) {
        return i -> createFromObject(i, returnType);
    }
}
