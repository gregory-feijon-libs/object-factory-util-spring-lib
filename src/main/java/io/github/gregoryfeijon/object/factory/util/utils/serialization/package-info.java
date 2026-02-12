/**
 * Public API for deep object copy operations.
 * <p>
 * The main entry point is {@link io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil},
 * which provides static methods for copying individual objects, collections, and maps
 * with full support for type conversion, field exclusion, and Hibernate proxy unwrapping.
 * </p>
 * <p>
 * {@link io.github.gregoryfeijon.object.factory.util.utils.serialization.HibernateProxyChecker}
 * provides optional Hibernate integration via reflection, allowing the library to work
 * both with and without Hibernate on the classpath.
 * </p>
 *
 * @author gregory.feijon
 * @see io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil
 */
package io.github.gregoryfeijon.object.factory.util.utils.serialization;
