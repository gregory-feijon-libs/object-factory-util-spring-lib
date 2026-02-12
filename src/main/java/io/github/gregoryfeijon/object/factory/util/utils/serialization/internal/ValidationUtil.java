package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.object.factory.util.exception.ErrorMessages;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Input validation utilities for the object copy operations.
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtil {

    /**
     * Verifies that both source and destination objects are non-null.
     *
     * @param <T>    the type of the destination object
     * @param <S>    the type of the source object
     * @param source the source object
     * @param dest   the destination object
     * @throws ApiException if either object is null
     */
    public static <T, S> void verifySourceAndDestObjects(S source, T dest) {
        verifySourceObject(source);
        if (dest == null) {
            throw new ApiException(ErrorMessages.DESTINATION_OBJECT_NULL);
        }
    }

    /**
     * Verifies that the source object to be copied is non-null.
     *
     * @param <S>    the type of the source object
     * @param source the source object
     * @throws ApiException if the source object is null
     */
    public static <S> void verifySourceObject(S source) {
        if (source == null) {
            throw new ApiException(ErrorMessages.SOURCE_OBJECT_NULL);
        }
    }

    /**
     * Verifies that a collection is not empty.
     *
     * @param <T>        the type of objects in the collection
     * @param collection the collection to verify
     * @throws ApiException if the collection is empty
     */
    public static <T> void verifyCollection(Collection<T> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new ApiException(ErrorMessages.COLLECTION_EMPTY);
        }
    }

    /**
     * Verifies that both a collection and a supplier are non-null.
     *
     * @param <T>        the type of objects in the collection
     * @param <U>        the type of collection to be created by the supplier
     * @param collection the collection to verify
     * @param supplier   the supplier to verify
     * @throws ApiException if either the collection is empty or the supplier is null
     */
    public static <T, U> void verifyCollectionAndSupplier(Collection<T> collection, Supplier<U> supplier) {
        verifyCollection(collection);
        if (supplier == null) {
            throw new ApiException(ErrorMessages.SUPPLIER_NULL);
        }
    }
}
