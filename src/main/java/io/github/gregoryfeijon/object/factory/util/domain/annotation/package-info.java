/**
 * Annotations for controlling object copy behavior.
 * <p>
 * These annotations allow fine-grained control over which fields are
 * included or excluded during deep copy operations:
 * </p>
 * <ul>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName} -
 *       Custom field name mapping between source and destination</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude} -
 *       Field-level exclusion from copy operations</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions} -
 *       Class-level field exclusion list</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectConstructor} -
 *       Class-level configuration including exclusion support</li>
 * </ul>
 *
 * @author gregory.feijon
 */
package io.github.gregoryfeijon.object.factory.util.domain.annotation;
