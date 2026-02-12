package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.model.ClassPairKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe cache for field resolution results.
 * <p>
 * Stores two types of cached data:
 * </p>
 * <ul>
 *   <li>Field key maps per class (normalized field name → Field)</li>
 *   <li>Copyable field lists per source-destination class pair</li>
 * </ul>
 * <p>
 * Both caches use {@link ConcurrentHashMap} with atomic {@code computeIfAbsent}
 * operations for thread safety.
 * </p>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CopyCache {

    private static final Map<Class<?>, Map<String, Field>> FIELD_KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<ClassPairKey, List<Field>> FIELDS_TO_COPY_CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves or computes the field key map for a given class.
     *
     * @param clazz           the class to resolve
     * @param mappingFunction the function to compute the map if absent
     * @return the cached or computed field key map
     */
    public static Map<String, Field> getOrComputeFieldKeyMap(Class<?> clazz,
                                                              Function<Class<?>, Map<String, Field>> mappingFunction) {
        return FIELD_KEY_CACHE.computeIfAbsent(clazz, mappingFunction);
    }

    /**
     * Retrieves or computes the list of copyable fields for a class pair.
     *
     * @param key             the source-destination class pair key
     * @param mappingFunction the function to compute the list if absent
     * @return the cached or computed list of copyable fields
     */
    public static List<Field> getOrComputeFieldsToCopy(ClassPairKey key,
                                                        Function<ClassPairKey, List<Field>> mappingFunction) {
        return FIELDS_TO_COPY_CACHE.computeIfAbsent(key, mappingFunction);
    }
}
