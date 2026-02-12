package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.BarWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.EmptySource;
import io.github.gregoryfeijon.object.factory.util.domain.FooDuplicated;
import io.github.gregoryfeijon.object.factory.util.domain.FooWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.MismatchSource;
import io.github.gregoryfeijon.object.factory.util.domain.MismatchTarget;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectBar;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectFoo;
import io.github.gregoryfeijon.object.factory.util.domain.OnlyStaticAttributeDestination;
import io.github.gregoryfeijon.object.factory.util.domain.OnlyStaticAttributeSource;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveBar;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldResolverTest {

    @Test
    void createSourceDestFieldMaps_shouldMapMatchingFields() {
        PrimitiveFoo source = new PrimitiveFoo();
        PrimitiveBar dest = new PrimitiveBar();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        // PrimitiveFoo has 3 fields, PrimitiveBar has 3 fields with @FieldCopyName mapping
        assertThat(fieldMap)
                .isNotEmpty()
                .hasSize(3);
    }

    @Test
    void createSourceDestFieldMaps_shouldReturnEmptyForNoMatch() {
        MismatchSource source = new MismatchSource();
        MismatchTarget dest = new MismatchTarget();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        assertThat(fieldMap).isEmpty();
    }

    @Test
    void createSourceDestFieldMaps_shouldReturnEmptyWhenOnlyStaticFields() {
        OnlyStaticAttributeSource source = new OnlyStaticAttributeSource();
        OnlyStaticAttributeDestination dest = new OnlyStaticAttributeDestination();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        assertThat(fieldMap).isEmpty();
    }

    @Test
    void createSourceDestFieldMaps_shouldExcludeFieldsWithAnnotation() {
        FooWrapper source = new FooWrapper();
        BarWrapper dest = new BarWrapper();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        // fieldExcluded (via @ObjectConstructor on BarWrapper), fieldExcludedWithAnnotation (via @ObjectCopyExclude on FooWrapper),
        // fieldExcludedWithAnnotationInDest (via @ObjectCopyExclude on BarWrapper), fieldExcludedUsingClassLevelAnnotation (via @ObjectCopyExclusions on FooWrapper)
        // should all be excluded. Total FooWrapper fields = 11, excluded = 4 => 7 mapped
        // But attributeNameDoesntExists exclusion is a no-op. fieldExcluded excluded by @ObjectConstructor on dest.
        // alternativeName excluded by @ObjectCopyExclusions on source.
        // @ObjectCopyExclude on fieldExcludedWithAnnotation in source.
        // @ObjectCopyExclude on fieldExcludedWithAnnotationInDestNameModified in dest.
        // So 4 fields excluded => remaining should map
        boolean hasExcludedField = fieldMap.keySet().stream()
                .anyMatch(f -> f.getName().equals("fieldExcluded")
                        || f.getName().equals("fieldExcludedWithAnnotation")
                        || f.getName().equals("fieldExcludedUsingClassLevelAnnotation"));
        assertThat(hasExcludedField).isFalse();
    }

    @Test
    void resolveFieldKey_shouldUseFieldNameWhenNoAnnotation() throws Exception {
        Field field = PrimitiveFoo.class.getDeclaredField("intValue");

        String key = FieldResolver.resolveFieldKey(field);

        assertThat(key).isEqualTo("intvalue");
    }

    @Test
    void resolveFieldKey_shouldUseAnnotationValueWhenPresent() throws Exception {
        Field field = PrimitiveBar.class.getDeclaredField("iVal");

        String key = FieldResolver.resolveFieldKey(field);

        assertThat(key).isEqualTo("intvalue");
    }

    @Test
    void buildFieldKeyMap_shouldGroupByDeclaringClass() throws Exception {
        Field f1 = PrimitiveFoo.class.getDeclaredField("intValue");
        Field f2 = PrimitiveFoo.class.getDeclaredField("longValue");

        Map<String, Field> result = FieldResolver.buildFieldKeyMap(List.of(f1, f2));

        // CopyCache caches by declaring class, so all 3 fields of PrimitiveFoo are cached together
        assertThat(result)
                .containsKey("intvalue")
                .containsKey("longvalue");
    }

    @Test
    void getFieldsToCopy_shouldExcludeStaticFinalFields() {
        OnlyStaticAttributeSource source = new OnlyStaticAttributeSource();
        OnlyStaticAttributeDestination dest = new OnlyStaticAttributeDestination();

        List<Field> fields = FieldResolver.getFieldsToCopy(source, dest);

        assertThat(fields).isEmpty();
    }

    @Test
    void createSourceDestFieldMaps_shouldHandleDuplicateFieldKeys() {
        FooDuplicated source = new FooDuplicated();
        FooDuplicated dest = new FooDuplicated();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        // Both nome1 and nome2 have @FieldCopyName("duplicado"), so the merge function
        // keeps the first one. The map should still contain the duplicated key mapping.
        assertThat(fieldMap).isNotEmpty();
    }

    @Test
    void createSourceDestFieldMaps_shouldMapAnnotatedFieldsBetweenDifferentTypes() {
        ObjectFoo source = new ObjectFoo();
        ObjectBar dest = new ObjectBar();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        // ObjectFoo has: integerValue, stringValue, bigDecimalValue
        // ObjectBar has: integerValue, stringValue, bdValue(@FieldCopyName("bigDecimalValue"))
        assertThat(fieldMap).hasSize(3);
    }

    @Test
    void createSourceDestFieldMaps_shouldReturnEmptyForEmptySource() {
        EmptySource source = new EmptySource();
        PrimitiveBar dest = new PrimitiveBar();

        Map<Field, Field> fieldMap = FieldResolver.createSourceDestFieldMaps(source, dest);

        assertThat(fieldMap).isEmpty();
    }
}
