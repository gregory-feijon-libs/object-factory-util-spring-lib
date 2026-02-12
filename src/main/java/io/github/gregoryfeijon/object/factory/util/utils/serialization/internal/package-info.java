/**
 * Internal implementation classes for the object copy library.
 * <p>
 * <strong>This package is not part of the public API.</strong> Classes in this package
 * are implementation details and may change without notice between versions.
 * </p>
 * <p>
 * The following delegate classes decompose the copy logic:
 * </p>
 * <ul>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.CopyCache} -
 *       Thread-safe caching of field resolution results</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.ValidationUtil} -
 *       Input validation</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.EnumConverter} -
 *       Enum type conversions</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.TypeResolver} -
 *       Generic type resolution</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.FieldResolver} -
 *       Field matching and exclusion logic</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.HibernateProxyHandler} -
 *       Hibernate proxy unwrapping</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.ObjectCloner} -
 *       Binary and JSON serialization cloning</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.CollectionMapCloner} -
 *       Deep copy of collections and maps</li>
 *   <li>{@link io.github.gregoryfeijon.object.factory.util.utils.serialization.internal.CopyOrchestrator} -
 *       Central dispatch for field value processing</li>
 * </ul>
 *
 * @author gregory.feijon
 */
package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;
