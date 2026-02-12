package io.github.gregoryfeijon.object.factory.util.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Centralized error messages for the ObjectFactoryUtil library.
 * <p>
 * This class provides constant error messages used throughout the library,
 * ensuring consistency and facilitating maintenance and internationalization.
 * </p>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessages {

    /**
     * Error message when the source object is null.
     */
    public static final String SOURCE_OBJECT_NULL = "The object to be copied is null.";

    /**
     * Error message when the destination object is null.
     */
    public static final String DESTINATION_OBJECT_NULL = "The destination object is null.";

    /**
     * Error message when the collection to be copied is empty.
     */
    public static final String COLLECTION_EMPTY = "The collection to be copied has no elements.";

    /**
     * Error message when the supplier for collection type is null.
     */
    public static final String SUPPLIER_NULL = "The specified collection type for return is null.";

    /**
     * Error message when cloning a collection or map fails.
     */
    public static final String CLONE_COLLECTION_MAP_ERROR = "Error cloning collection/map during object copy.";
}
