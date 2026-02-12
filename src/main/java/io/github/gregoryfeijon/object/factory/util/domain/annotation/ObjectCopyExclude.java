package io.github.gregoryfeijon.object.factory.util.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be excluded from copy operations.
 * <p>
 * This annotation can be applied to fields in either the source or destination class:
 * </p>
 * <ul>
 *   <li><b>On source field:</b> The field will never be copied to any destination</li>
 *   <li><b>On destination field:</b> The corresponding source field (matched by name
 *       or {@link FieldCopyName}) will not be copied to this field</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * public class User {
 *     private String username;
 *
 *     &#64;ObjectCopyExclude
 *     private String password;  // Will never be copied
 *
 *     &#64;ObjectCopyExclude
 *     private String sessionToken;  // Will never be copied
 * }
 * </pre>
 *
 * @author gregory.feijon
 * @see ObjectCopyExclusions
 * @see ObjectConstructor#exclude()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObjectCopyExclude {
}
