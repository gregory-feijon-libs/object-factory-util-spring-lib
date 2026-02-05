package io.github.gregoryfeijon.object.factory.util.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation to specify fields that should be excluded from copy operations.
 * <p>
 * Unlike {@link ObjectConstructor#exclude()}, this annotation can be applied to both
 * source and destination classes. Exclusions defined here are inherited from superclasses.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * &#64;ObjectCopyExclusions({"tempData", "cache", "internalState"})
 * public class DataContainer {
 *     private String tempData;      // Won't be copied
 *     private Map cache;            // Won't be copied
 *     private Object internalState; // Won't be copied
 *     private String name;          // Will be copied normally
 * }
 * </pre>
 *
 * @author gregory.feijon
 * @see ObjectCopyExclude
 * @see ObjectConstructor#exclude()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ObjectCopyExclusions {

    /**
     * Array of field names to be excluded from object copy operations.
     * <p>
     * Field names are matched case-insensitively against the actual field names
     * or their {@link FieldCopyName} mappings.
     * </p>
     *
     * @return the field names to exclude
     */
    String[] value() default {};
}