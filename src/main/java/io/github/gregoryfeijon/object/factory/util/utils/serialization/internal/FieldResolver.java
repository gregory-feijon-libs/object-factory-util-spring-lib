package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.ReflectionUtil;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectConstructor;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import io.github.gregoryfeijon.object.factory.util.domain.model.ClassPairKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * Resolves and matches fields between source and destination objects,
 * applying exclusion rules and annotation-based mappings.
 *
 * @author gregory.feijon
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldResolver {

    private static final Predicate<Field> PREDICATE_MODIFIERS = predicateModifiers();

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
        var sourceFields = getFieldsToCopy(source, dest);
        var destFields = ReflectionUtil.getFieldsAsCollection(dest, ArrayList::new);

        if (sourceFields.isEmpty() || destFields.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Field> sourceFieldMap = buildFieldKeyMap(sourceFields);
        Map<String, Field> destFieldMap = buildFieldKeyMap(destFields);

        return sourceFieldMap.entrySet().stream()
                .filter(entry -> destFieldMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        entry -> destFieldMap.get(entry.getKey())
                ));
    }

    /**
     * Builds a key-to-field map for a given class, using cached data when available.
     *
     * @param fields the list of fields to process
     * @return a map of normalized field names to {@link Field} objects
     */
    @SuppressWarnings("java:S6204")
    static Map<String, Field> buildFieldKeyMap(List<Field> fields) {
        return fields.stream()
                .collect(Collectors.groupingBy(Field::getDeclaringClass))
                .entrySet().stream()
                .flatMap(entry -> CopyCache.getOrComputeFieldKeyMap(
                        entry.getKey(),
                        cls -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        FieldResolver::resolveFieldKey,
                                        Function.identity(),
                                        (a, b) -> {
                                            log.warn("Duplicate field key '{}' detected in class '{}'. Keeping first occurrence.",
                                                    a.getName(), cls.getSimpleName());
                                            return a;
                                        })
                                )
                ).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Resolves the logical key name for a field, considering the {@link FieldCopyName} annotation.
     *
     * @param field the field to resolve
     * @return the normalized key representing the logical field name
     */
    public static String resolveFieldKey(Field field) {
        FieldCopyName ann = field.getAnnotation(FieldCopyName.class);
        String key = (ann != null && StringUtils.hasText(ann.value()))
                ? ann.value()
                : field.getName();
        return key.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Retrieves all fields that should be copied from the source object,
     * applying caching and exclusion rules.
     *
     * @param <T>    the type of the destination object
     * @param <S>    the type of the source object
     * @param source the source instance to copy fields from
     * @param dest   the destination instance to copy fields to
     * @return a cached or computed list of copyable fields
     */
    @SuppressWarnings("java:S6204")
    static <T, S> List<Field> getFieldsToCopy(S source, T dest) {
        ClassPairKey cacheKey = new ClassPairKey(source.getClass(), dest.getClass());

        return CopyCache.getOrComputeFieldsToCopy(cacheKey, key -> {
            List<Field> sourceFields = new ArrayList<>(ReflectionUtil.getFieldsAsCollection(source));
            Set<Field> fieldsToRemove = sourceFields.stream()
                    .filter(PREDICATE_MODIFIERS)
                    .collect(Collectors.toSet());

            addAnnotationBasedExclusions(fieldsToRemove, sourceFields, dest, true);
            addAnnotationBasedExclusions(fieldsToRemove, sourceFields, source, false);

            excludeAnnotatedSourceFields(fieldsToRemove, sourceFields);
            excludeAnnotatedDestinationFields(fieldsToRemove, sourceFields, dest);

            if (!fieldsToRemove.isEmpty()) {
                sourceFields.removeAll(fieldsToRemove);
            }

            return List.copyOf(sourceFields);
        });
    }

    private static void addAnnotationBasedExclusions(Set<Field> fieldsToRemove,
                                                      List<Field> sourceFields,
                                                      Object target,
                                                      boolean includeObjectConstructor) {
        Set<String> excludeFields = getClassExclusions(target.getClass(), includeObjectConstructor);

        if (!excludeFields.isEmpty()) {
            excludeListedFields(fieldsToRemove, sourceFields, excludeFields.toArray(new String[0]));
        }
    }

    private static Set<String> getClassExclusions(Class<?> clazz, boolean includeObjectConstructor) {
        Set<String> exclusions = new HashSet<>();

        while (clazz != null && clazz != Object.class) {
            if (includeObjectConstructor && clazz.isAnnotationPresent(ObjectConstructor.class)) {
                exclusions.addAll(Arrays.asList(clazz.getAnnotation(ObjectConstructor.class).exclude()));
            }
            if (clazz.isAnnotationPresent(ObjectCopyExclusions.class)) {
                exclusions.addAll(Arrays.asList(clazz.getAnnotation(ObjectCopyExclusions.class).value()));
            }
            clazz = clazz.getSuperclass();
        }

        return Set.copyOf(exclusions);
    }

    private static void excludeAnnotatedSourceFields(Set<Field> fieldsToRemove, List<Field> sourceFields) {
        sourceFields.stream()
                .filter(f -> f.isAnnotationPresent(ObjectCopyExclude.class))
                .forEach(fieldsToRemove::add);
    }

    private static <T> void excludeAnnotatedDestinationFields(Set<Field> fieldsToRemove,
                                                               List<Field> sourceFields,
                                                               T dest) {
        List<Field> destFields = ReflectionUtil.getFieldsAsCollection(dest, ArrayList::new);

        Map<String, Field> sourceFieldMap = sourceFields.stream()
                .collect(Collectors.toMap(
                        FieldResolver::resolveFieldKey,
                        Function.identity(),
                        (a, b) -> a
                ));

        destFields.stream()
                .filter(f -> f.isAnnotationPresent(ObjectCopyExclude.class))
                .map(FieldResolver::resolveFieldKey)
                .map(sourceFieldMap::get)
                .filter(Objects::nonNull)
                .forEach(fieldsToRemove::add);
    }

    private static void excludeListedFields(Set<Field> fieldsToRemove,
                                             List<Field> sourceFields,
                                             String[] exclude) {
        stream(exclude)
                .forEach(excludeField -> sourceFields.stream()
                        .filter(sourceField -> resolveFieldKey(sourceField).equalsIgnoreCase(excludeField))
                        .findAny()
                        .ifPresentOrElse(fieldsToRemove::add,
                                () -> log.trace("ObjectCopyExclusions: field '{}' not found in source class. Skipping" +
                                        " exclusion.", excludeField)
                        )
                );
    }

    private static Predicate<Field> predicateModifiers() {
        return p -> Modifier.isStatic(p.getModifiers()) && Modifier.isFinal(p.getModifiers());
    }
}
