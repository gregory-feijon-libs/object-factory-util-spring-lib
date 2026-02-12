package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectBar;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectFoo;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveBar;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.object.factory.util.util.TestObjectsFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        FactoryUtil.class,
        TestSerializerConfiguration.class
})
class CollectionMapClonerTest {

    // Fields used for generic type extraction via reflection
    @SuppressWarnings("unused")
    private List<PrimitiveFoo> primitiveFooList;
    @SuppressWarnings("unused")
    private List<PrimitiveBar> primitiveBarList;
    @SuppressWarnings("unused")
    private List<String> stringList;
    @SuppressWarnings("unused")
    private List<List<PrimitiveFoo>> nestedPrimitiveFooList;
    @SuppressWarnings("unused")
    private List<List<PrimitiveBar>> nestedPrimitiveBarList;
    @SuppressWarnings("unused")
    private Map<String, ObjectFoo> stringObjectFooMap;
    @SuppressWarnings("unused")
    private Map<String, ObjectBar> stringObjectBarMap;
    @SuppressWarnings("unused")
    private Map<String, String> stringStringMap;
    @SuppressWarnings("unused")
    private Map<Integer, Map<Integer, PrimitiveFoo>> nestedPrimitiveFooMap;
    @SuppressWarnings("unused")
    private Map<Integer, Map<Integer, PrimitiveBar>> nestedPrimitiveBarMap;

    private Type genericTypeOf(String fieldName) throws NoSuchFieldException {
        Field f = CollectionMapClonerTest.class.getDeclaredField(fieldName);
        return f.getGenericType();
    }

    // ==================== null handling ====================

    @Test
    void serializingCloneCollectionMap_shouldReturnNull_whenSourceIsNull() throws Exception {
        Type type = genericTypeOf("stringList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(null, type);

        assertThat(result).isNull();
    }

    // ==================== simple collection - same type ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneSimpleList_withSameElementType() throws Exception {
        List<PrimitiveFoo> source = List.of(TestObjectsFactory.createPrimitiveFoo());
        Type type = genericTypeOf("primitiveFooList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<PrimitiveFoo> cloned = (List<PrimitiveFoo>) result;
        assertThat(cloned).hasSize(1);
        assertThat(cloned.getFirst().getIntValue()).isEqualTo(1);
        assertThat(cloned.getFirst().getLongValue()).isEqualTo(2L);
        assertThat(cloned.getFirst().isBoolValue()).isTrue();
    }

    @Test
    void serializingCloneCollectionMap_shouldDeepCloneList_elementsAreNewReferences() throws Exception {
        PrimitiveFoo original = TestObjectsFactory.createPrimitiveFoo();
        List<PrimitiveFoo> source = new ArrayList<>(List.of(original));
        Type type = genericTypeOf("primitiveFooList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        @SuppressWarnings("unchecked")
        List<PrimitiveFoo> cloned = (List<PrimitiveFoo>) result;
        assertThat(cloned.getFirst()).isNotSameAs(original);
    }

    // ==================== simple collection - different type ====================

    @Test
    void serializingCloneCollectionMap_shouldConvertElementTypes_whenDifferent() throws Exception {
        List<PrimitiveFoo> source = List.of(TestObjectsFactory.createPrimitiveFoo());
        Type type = genericTypeOf("primitiveBarList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<PrimitiveBar> cloned = (List<PrimitiveBar>) result;
        assertThat(cloned).hasSize(1);
        assertThat(cloned.getFirst().getLongValue()).isEqualTo(2L);
        assertThat(cloned.getFirst().isBoolValue()).isTrue();
    }

    // ==================== empty collection ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneEmptyList() throws Exception {
        List<String> source = Collections.emptyList();
        Type type = genericTypeOf("stringList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> cloned = (List<String>) result;
        assertThat(cloned).isEmpty();
    }

    // ==================== nested collection - same type ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneNestedList_withSameInnermostType() throws Exception {
        PrimitiveFoo foo = TestObjectsFactory.createPrimitiveFoo();
        List<List<PrimitiveFoo>> source = List.of(List.of(foo));
        Type type = genericTypeOf("nestedPrimitiveFooList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<List<PrimitiveFoo>> cloned = (List<List<PrimitiveFoo>>) result;
        assertThat(cloned).hasSize(1);
        assertThat(cloned.getFirst()).hasSize(1);
        assertThat(cloned.getFirst().getFirst().getIntValue()).isEqualTo(1);
    }

    // ==================== nested collection - different type ====================

    @Test
    void serializingCloneCollectionMap_shouldConvertNestedList_whenInnermostTypeDiffers() throws Exception {
        PrimitiveFoo foo = TestObjectsFactory.createPrimitiveFoo();
        List<List<PrimitiveFoo>> source = List.of(List.of(foo));
        Type type = genericTypeOf("nestedPrimitiveBarList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Object> cloned = (List<Object>) result;
        assertThat(cloned).hasSize(1);
    }

    // ==================== simple map - same type ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneSimpleMap_withSameValueType() throws Exception {
        ObjectFoo foo = TestObjectsFactory.createObjectFoo();
        Map<String, ObjectFoo> source = Map.of("key", foo);
        Type type = genericTypeOf("stringObjectFooMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, ObjectFoo> cloned = (Map<String, ObjectFoo>) result;
        assertThat(cloned).containsKey("key");
        assertThat(cloned.get("key").getStringValue()).isEqualTo("foo");
        assertThat(cloned.get("key").getIntegerValue()).isEqualTo(10);
    }

    @Test
    void serializingCloneCollectionMap_shouldDeepCloneMap_valuesAreNewReferences() throws Exception {
        ObjectFoo foo = TestObjectsFactory.createObjectFoo();
        Map<String, ObjectFoo> source = new HashMap<>(Map.of("key", foo));
        Type type = genericTypeOf("stringObjectFooMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        @SuppressWarnings("unchecked")
        Map<String, ObjectFoo> cloned = (Map<String, ObjectFoo>) result;
        assertThat(cloned.get("key")).isNotSameAs(foo);
    }

    // ==================== simple map - different type ====================

    @Test
    void serializingCloneCollectionMap_shouldConvertMapValueTypes_whenDifferent() throws Exception {
        ObjectFoo foo = TestObjectsFactory.createObjectFoo();
        Map<String, ObjectFoo> source = Map.of("key", foo);
        Type type = genericTypeOf("stringObjectBarMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, ObjectBar> cloned = (Map<String, ObjectBar>) result;
        assertThat(cloned.get("key")).isInstanceOf(ObjectBar.class);
        assertThat(cloned.get("key").getStringValue()).isEqualTo("foo");
        assertThat(cloned.get("key").getBdValue()).isEqualTo(BigDecimal.valueOf(100.5));
    }

    // ==================== empty map ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneEmptyMap() throws Exception {
        Map<String, String> source = Collections.emptyMap();
        Type type = genericTypeOf("stringStringMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> cloned = (Map<String, String>) result;
        assertThat(cloned).isEmpty();
    }

    // ==================== nested map - same type ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneNestedMap_withSameInnermostType() throws Exception {
        PrimitiveFoo foo = TestObjectsFactory.createPrimitiveFoo();
        Map<Integer, Map<Integer, PrimitiveFoo>> source = Map.of(1, Map.of(2, foo));
        Type type = genericTypeOf("nestedPrimitiveFooMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<Integer, Map<Integer, PrimitiveFoo>> cloned =
                (Map<Integer, Map<Integer, PrimitiveFoo>>) result;
        assertThat(cloned.get(1).get(2).getIntValue()).isEqualTo(1);
    }

    // ==================== nested map - different type ====================

    @Test
    void serializingCloneCollectionMap_shouldConvertNestedMap_whenInnermostTypeDiffers() throws Exception {
        PrimitiveFoo foo = TestObjectsFactory.createPrimitiveFoo();
        Map<Integer, Map<Integer, PrimitiveFoo>> source = Map.of(1, Map.of(2, foo));
        Type type = genericTypeOf("nestedPrimitiveBarMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> cloned = (Map<Integer, Object>) result;
        assertThat(cloned).containsKey(1);
    }

    // ==================== multiple elements ====================

    @Test
    void serializingCloneCollectionMap_shouldCloneListWithMultipleElements() throws Exception {
        List<PrimitiveFoo> source = List.of(
                PrimitiveFoo.builder().intValue(1).longValue(10L).boolValue(true).build(),
                PrimitiveFoo.builder().intValue(2).longValue(20L).boolValue(false).build(),
                PrimitiveFoo.builder().intValue(3).longValue(30L).boolValue(true).build()
        );
        Type type = genericTypeOf("primitiveFooList");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        @SuppressWarnings("unchecked")
        List<PrimitiveFoo> cloned = (List<PrimitiveFoo>) result;
        assertThat(cloned).hasSize(3);
        assertThat(cloned.get(0).getIntValue()).isEqualTo(1);
        assertThat(cloned.get(1).getIntValue()).isEqualTo(2);
        assertThat(cloned.get(2).getIntValue()).isEqualTo(3);
    }

    @Test
    void serializingCloneCollectionMap_shouldCloneMapWithMultipleEntries() throws Exception {
        Map<String, ObjectFoo> source = Map.of(
                "a", ObjectFoo.builder().integerValue(1).stringValue("aa").bigDecimalValue(BigDecimal.ONE).build(),
                "b", ObjectFoo.builder().integerValue(2).stringValue("bb").bigDecimalValue(BigDecimal.TEN).build()
        );
        Type type = genericTypeOf("stringObjectFooMap");

        Object result = CollectionMapCloner.serializingCloneCollectionMap(source, type);

        @SuppressWarnings("unchecked")
        Map<String, ObjectFoo> cloned = (Map<String, ObjectFoo>) result;
        assertThat(cloned).hasSize(2);
        assertThat(cloned).containsKeys("a", "b");
    }

}
