package io.github.gregoryfeijon.object.factory.util.domain.annotation;

import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that provides configuration for object construction during deep copying.
 * <p>
 * This annotation is used by {@link ObjectFactoryUtil}
 * to control which fields should be excluded when creating a copy of an object.
 * </p>
 * <p>
 * <b>Important:</b> This annotation is only evaluated on the <em>destination</em> class.
 * For exclusions on the source class, use {@link ObjectCopyExclusions} instead.
 * </p>
 * <p>
 * Exclusions defined in superclasses are inherited and applied to subclasses.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * &#64;ObjectConstructor(exclude = {"password", "temporaryData"})
 * public class User {
 *     private String username;
 *     private String password;
 *     private Map&lt;String, Object&gt; temporaryData;
 * }
 * </pre>
 *
 * @author gregory.feijon
 * @see ObjectCopyExclusions
 * @see ObjectCopyExclude
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ObjectConstructor {

    /**
     * Specifies field names that should be excluded during object copying.
     * <p>
     * Fields listed here will not be copied from the source object to the
     * destination object when using {@link ObjectFactoryUtil}.
     * </p>
     * <p>
     * Field names are matched case-insensitively against the actual field names
     * or their {@link FieldCopyName} mappings.
     * </p>
     *
     * @return array of field names to exclude
     */
    String[] exclude() default {};
}