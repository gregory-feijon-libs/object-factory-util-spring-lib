package io.github.gregoryfeijon.object.factory.util.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a different name during copy operations.
 * <p>
 * Use this annotation when source and destination fields have different names
 * but should be matched during the copy process. The matching is case-insensitive.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * public class Source {
 *     private int intValue;
 * }
 *
 * public class Target {
 *     &#64;FieldCopyName("intValue")
 *     private int iVal;  // Will receive value from 'intValue' field
 * }
 * </pre>
 *
 * @author gregory.feijon
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldCopyName {

    /**
     * The name of the corresponding field in the source or destination object.
     * <p>
     * This value is matched case-insensitively against field names in the
     * counterpart object during copy operations.
     * </p>
     *
     * @return the field name to map to
     */
    String value();
}

