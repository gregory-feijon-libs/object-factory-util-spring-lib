package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.model.ClassPairKey;
import io.github.gregoryfeijon.object.factory.util.domain.model.FieldMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe memoization of field resolution results.
 * <p>
 * Caches the complete, already-filtered list of {@link FieldMapping}s per
 * (source class, destination class) pair. Keying by the pair (and never by a single class)
 * guarantees that exclusion rules configured for one pair cannot leak into another.
 * </p>
 * <p>
 * Uses {@link ConcurrentHashMap#computeIfAbsent} for atomic, lock-striped computation.
 * </p>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CopyCache {

    private static final Map<ClassPairKey, List<FieldMapping>> FIELD_MAPPINGS_CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves or computes the field mappings for a class pair.
     *
     * @param key             the source-destination class pair key
     * @param mappingFunction the function to compute the mappings if absent
     * @return the cached or computed (immutable) list of field mappings
     */
    public static List<FieldMapping> getOrComputeFieldMappings(ClassPairKey key,
                                                               Function<ClassPairKey, List<FieldMapping>> mappingFunction) {
        return FIELD_MAPPINGS_CACHE.computeIfAbsent(key, mappingFunction);
    }
}
