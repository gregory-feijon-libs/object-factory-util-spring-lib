package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.ReflectionUtil;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectConstructor;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import io.github.gregoryfeijon.object.factory.util.domain.model.ClassPairKey;
import io.github.gregoryfeijon.object.factory.util.domain.model.FieldMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves and matches fields between source and destination objects,
 * applying exclusion rules and annotation-based mappings.
 * <p>
 * Matching is hierarchy-aware, so that hidden (shadowed) fields keep their own state:
 * </p>
 * <ol>
 *   <li><b>Exact match</b>: same logical key <em>and</em> same declaring class. Copies within the
 *       same hierarchy therefore preserve every slot, including a parent field hidden by a child.</li>
 *   <li><b>Name fallback</b>: remaining fields are paired by logical key in hierarchy order
 *       (most-derived first), mirroring how Java resolves {@code obj.field}. When the source has
 *       more fields for a key than the destination can receive, the extra ones are skipped with a warning.</li>
 * </ol>
 * <p>
 * Destination fields annotated with {@link ObjectCopyExclude} and static final fields never receive values.
 * The result depends only on the (source class, destination class) pair, so it is memoized in
 * {@link CopyCache} and computed exactly once per pair.
 * </p>
 *
 * @author gregory.feijon
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldResolver {

    /**
     * Identity of a field slot inside a hierarchy: its declaring class plus its logical key.
     */
    private record FieldSlot(Class<?> declaringClass, String key) {
        static FieldSlot of(Field field) {
            return new FieldSlot(field.getDeclaringClass(), resolveFieldKey(field));
        }
    }

    /**
     * Returns the cached field mappings between the classes of the given objects,
     * computing them on first use.
     *
     * @param source the source object whose fields are to be copied
     * @param dest   the destination object that will receive the values
     * @return an immutable list of source/destination field pairs
     */
    public static List<FieldMapping> resolveFieldMappings(Object source, Object dest) {
        ClassPairKey key = new ClassPairKey(source.getClass(), dest.getClass());
        return CopyCache.getOrComputeFieldMappings(key, k -> computeFieldMappings(source, dest));
    }

    /**
     * Creates a mapping between source and destination fields that share
     * the same logical name, considering {@link FieldCopyName} annotations.
     *
     * @param <S>    the source object type
     * @param <T>    the destination object type
     * @param source the source object whose fields are to be copied
     * @param dest   the destination object that will receive the values
     * @return a map where the key is the source field and the value is the corresponding destination field
     */
    public static <S, T> Map<Field, Field> createSourceDestFieldMaps(S source, T dest) {
        return resolveFieldMappings(source, dest).stream()
                .collect(Collectors.toMap(FieldMapping::sourceField, FieldMapping::destField,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private static List<FieldMapping> computeFieldMappings(Object source, Object dest) {
        List<Field> sourceFields = getFieldsToCopy(source, dest);
        Set<Field> pendingDest = ReflectionUtil.getFieldsAsCollection(dest, LinkedHashSet::new);
        pendingDest.removeIf(FieldResolver::isStaticFinal);

        List<FieldMapping> mappings = new ArrayList<>();
        List<Field> pendingSource = matchBySlot(sourceFields, pendingDest, mappings);
        matchByKeyInHierarchyOrder(pendingSource, pendingDest, mappings, dest.getClass());

        return mappings.stream()
                .filter(mapping -> !mapping.destField().isAnnotationPresent(ObjectCopyExclude.class))
                .toList();
    }

    /**
     * Phase 1: pairs fields with the same declaring class and logical key.
     *
     * @return the source fields left unmatched, in their original order
     */
    private static List<Field> matchBySlot(List<Field> sourceFields, Set<Field> pendingDest,
                                           List<FieldMapping> mappings) {
        Map<FieldSlot, Field> destBySlot = new HashMap<>();
        pendingDest.forEach(field -> destBySlot.putIfAbsent(FieldSlot.of(field), field));

        List<Field> unmatched = new ArrayList<>();
        for (Field sourceField : sourceFields) {
            Field destField = destBySlot.remove(FieldSlot.of(sourceField));
            if (destField == null) {
                unmatched.add(sourceField);
            } else {
                mappings.add(new FieldMapping(sourceField, destField));
                pendingDest.remove(destField);
            }
        }
        return unmatched;
    }

    /**
     * Phase 2: pairs the remaining fields by logical key, most-derived first on both sides.
     */
    private static void matchByKeyInHierarchyOrder(List<Field> pendingSource, Set<Field> pendingDest,
                                                   List<FieldMapping> mappings, Class<?> destClass) {
        Map<String, Deque<Field>> destByKey = new HashMap<>();
        pendingDest.forEach(field ->
                destByKey.computeIfAbsent(resolveFieldKey(field), k -> new ArrayDeque<>()).add(field));

        for (Field sourceField : pendingSource) {
            Deque<Field> candidates = destByKey.get(resolveFieldKey(sourceField));
            if (candidates == null) {
                continue; // destination has no field with this key
            }
            if (candidates.isEmpty()) {
                warnUnassignable(sourceField, destClass);
            } else {
                mappings.add(new FieldMapping(sourceField, candidates.poll()));
            }
        }
    }

    private static void warnUnassignable(Field sourceField, Class<?> destClass) {
        log.warn("Field '{}.{}' resolves to key '{}', already taken by a more specific field in '{}'. Skipping it.",
                sourceField.getDeclaringClass().getSimpleName(), sourceField.getName(),
                resolveFieldKey(sourceField), destClass.getSimpleName());
    }

    /**
     * Resolves the logical key name for a field, considering the {@link FieldCopyName} annotation.
     *
     * @param field the field to resolve
     * @return the normalized key representing the logical field name
     */
    public static String resolveFieldKey(Field field) {
        FieldCopyName ann = field.getAnnotation(FieldCopyName.class);
        String key = (ann != null && StringUtils.hasText(ann.value())) ? ann.value() : field.getName();
        return normalizeKey(key);
    }

    /**
     * Retrieves all source fields that may be copied, applying source-side exclusion rules:
     * static final fields, {@link ObjectCopyExclude} on the source field, and class-level
     * {@link ObjectCopyExclusions} / {@link ObjectConstructor#exclude()} (matched by logical key,
     * so every field resolving to an excluded key is removed).
     *
     * @param <T>    the type of the destination object
     * @param <S>    the type of the source object
     * @param source the source instance to copy fields from
     * @param dest   the destination instance to copy fields to
     * @return an immutable list of copyable fields, in hierarchy order (most-derived first)
     */
    static <T, S> List<Field> getFieldsToCopy(S source, T dest) {
        Set<String> excludedKeys = new HashSet<>(getClassExclusions(dest.getClass(), true));
        excludedKeys.addAll(getClassExclusions(source.getClass(), false));

        return ReflectionUtil.getFieldsAsCollection(source, ArrayList::new).stream()
                .filter(field -> !isStaticFinal(field))
                .filter(field -> !field.isAnnotationPresent(ObjectCopyExclude.class))
                .filter(field -> !excludedKeys.contains(resolveFieldKey(field)))
                .toList();
    }

    private static Set<String> getClassExclusions(Class<?> clazz, boolean includeObjectConstructor) {
        Set<String> exclusions = new HashSet<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            ObjectConstructor constructor = current.getAnnotation(ObjectConstructor.class);
            if (includeObjectConstructor && constructor != null) {
                exclusions.addAll(Arrays.asList(constructor.exclude()));
            }
            ObjectCopyExclusions copyExclusions = current.getAnnotation(ObjectCopyExclusions.class);
            if (copyExclusions != null) {
                exclusions.addAll(Arrays.asList(copyExclusions.value()));
            }
        }
        return exclusions.stream().map(FieldResolver::normalizeKey).collect(Collectors.toSet());
    }

    private static boolean isStaticFinal(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).trim();
    }
}
