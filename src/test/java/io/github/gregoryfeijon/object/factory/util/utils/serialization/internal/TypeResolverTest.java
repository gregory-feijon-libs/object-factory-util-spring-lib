package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.serializer.provider.util.gson.GsonTypesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanInstantiationException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeResolverTest {

    @SuppressWarnings("unused")
    private List<String> stringList;
    @SuppressWarnings("unused")
    private Map<String, Integer> stringIntMap;
    @SuppressWarnings("unused")
    private List<List<String>> nestedList;
    @SuppressWarnings("unused")
    private List<List<List<String>>> deepNestedList;
    @SuppressWarnings("unused")
    private Map<String, List<Integer>> mapOfLists;
    @SuppressWarnings("unused")
    private Map<String, Runnable> stringRunnableMap;
    @SuppressWarnings("unused")
    private Map<Integer, PrimitiveFoo> intPrimitiveFooMap;

    private Type genericTypeOf(String fieldName) throws NoSuchFieldException {
        Field f = TypeResolverTest.class.getDeclaredField(fieldName);
        return f.getGenericType();
    }

    @Test
    void verifyAndExtractTypes_shouldExtractSingleTypeFromList() throws Exception {
        Type genericType = genericTypeOf("stringList");

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(genericType);

        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(String.class);
    }

    @Test
    void verifyAndExtractTypes_shouldExtractTwoTypesFromMap() throws Exception {
        Type genericType = genericTypeOf("stringIntMap");

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(genericType);

        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(String.class);
        assertThat(types[1]).isEqualTo(Integer.class);
    }

    @Test
    void verifyType_shouldNotThrowForValidType() throws Exception {
        Type genericType = genericTypeOf("stringList");

        TypeResolver.verifyType(genericType);
    }

    @Test
    void extractElementClass_shouldReturnClassForPlainClass() throws Exception {
        Class<?> result = TypeResolver.extractElementClass(String.class);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void extractElementClass_shouldReturnInnermostTypeForParameterizedType() throws Exception {
        Type genericType = genericTypeOf("nestedList");
        ParameterizedType paramType = (ParameterizedType) genericType;
        Type innerType = paramType.getActualTypeArguments()[0]; // List<String>

        Class<?> result = TypeResolver.extractElementClass(innerType);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void extractElementClass_shouldHandleDeepNesting() throws Exception {
        Type genericType = genericTypeOf("deepNestedList");
        ParameterizedType paramType = (ParameterizedType) genericType;
        // List<List<List<String>>> -> get first arg: List<List<String>>
        Type level2 = paramType.getActualTypeArguments()[0];

        Class<?> result = TypeResolver.extractElementClass(level2);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void extractCollectionElementType_shouldReturnElementType() throws Exception {
        Type genericType = genericTypeOf("stringList");

        Class<?> result = TypeResolver.extractCollectionElementType(genericType);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void extractMapValueType_shouldReturnValueType() throws Exception {
        Type genericType = genericTypeOf("stringIntMap");

        Class<?> result = TypeResolver.extractMapValueType(genericType);

        assertThat(result).isEqualTo(Integer.class);
    }

    @Test
    void getInnermostElementType_shouldReturnObjectClassForNull() {
        Class<?> result = TypeResolver.getInnermostElementType(null);

        assertThat(result).isEqualTo(Object.class);
    }

    @Test
    void getInnermostElementType_shouldReturnObjectClassForEmptyCollection() {
        Class<?> result = TypeResolver.getInnermostElementType(new ArrayList<>());

        assertThat(result).isEqualTo(Object.class);
    }

    @Test
    void getInnermostElementType_shouldReturnObjectClassForEmptyMap() {
        Class<?> result = TypeResolver.getInnermostElementType(new HashMap<>());

        assertThat(result).isEqualTo(Object.class);
    }

    @Test
    void getInnermostElementType_shouldReturnElementClassForSimpleCollection() {
        List<String> list = List.of("test");

        Class<?> result = TypeResolver.getInnermostElementType(list);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void getInnermostElementType_shouldReturnInnermostClassForNestedCollection() {
        List<List<String>> nested = List.of(List.of("test"));

        Class<?> result = TypeResolver.getInnermostElementType(nested);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void getInnermostElementType_shouldReturnValueClassForMap() {
        Map<String, Integer> map = Map.of("key", 42);

        Class<?> result = TypeResolver.getInnermostElementType(map);

        assertThat(result).isEqualTo(Integer.class);
    }

    @Test
    void getInnermostElementType_shouldReturnClassForPlainObject() {
        Class<?> result = TypeResolver.getInnermostElementType("test");

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void getNestedGenericType_shouldReturnTypeAtIndex() throws Exception {
        Type genericType = genericTypeOf("stringIntMap");

        Type keyType = TypeResolver.getNestedGenericType(genericType, 0);
        Type valueType = TypeResolver.getNestedGenericType(genericType, 1);

        assertThat(keyType).isEqualTo(String.class);
        assertThat(valueType).isEqualTo(Integer.class);
    }

    @Test
    void getNestedGenericType_shouldReturnObjectClassForOutOfBoundsIndex() throws Exception {
        Type genericType = genericTypeOf("stringList");

        Type result = TypeResolver.getNestedGenericType(genericType, 5);

        assertThat(result).isEqualTo(Object.class);
    }

    @Test
    void getNestedGenericType_shouldReturnObjectClassForNonParameterizedType() {
        Type result = TypeResolver.getNestedGenericType(String.class, 0);

        assertThat(result).isEqualTo(Object.class);
    }

    @Test
    void verifyAndExtractTypes_shouldNotThrowForInstantiableNestedType() throws Exception {
        Type genericType = genericTypeOf("mapOfLists");
        ParameterizedType paramType = (ParameterizedType) genericType;
        // Get List<Integer> from Map<String, List<Integer>>
        Type listType = paramType.getActualTypeArguments()[1]; // List<Integer>
        // Integer is instantiable, so no exception
        Class<?>[] types = TypeResolver.verifyAndExtractTypes(listType);
        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(Integer.class);
    }

    @Test
    void getInnermostElementType_shouldHandleNestedMap() {
        Map<String, Map<String, Integer>> nestedMap = Map.of("outer", Map.of("inner", 42));

        Class<?> result = TypeResolver.getInnermostElementType(nestedMap);

        assertThat(result).isEqualTo(Integer.class);
    }

    // ==================== exception paths ====================

    @Test
    void verifyAndExtractTypes_shouldThrowBeanInstantiationException_forNonInstantiableType() {
        // Runnable is an interface — cannot be instantiated by BeanUtils
        Type type = GsonTypesUtil.getType(List.class, Runnable.class);

        assertThatThrownBy(() -> TypeResolver.verifyAndExtractTypes(type))
                .isInstanceOf(BeanInstantiationException.class);
    }

    @Test
    void verifyType_shouldThrowBeanInstantiationException_forNonInstantiableType() {
        Type type = GsonTypesUtil.getType(List.class, Runnable.class);

        assertThatThrownBy(() -> TypeResolver.verifyType(type))
                .isInstanceOf(BeanInstantiationException.class);
    }

    @Test
    void extractCollectionElementType_shouldThrowBeanInstantiationException_forNonInstantiableType() {
        Type type = GsonTypesUtil.getType(List.class, Runnable.class);

        assertThatThrownBy(() -> TypeResolver.extractCollectionElementType(type))
                .isInstanceOf(BeanInstantiationException.class);
    }

    @Test
    void extractMapValueType_shouldThrowBeanInstantiationException_forNonInstantiableValueType() throws Exception {
        Type type = genericTypeOf("stringRunnableMap");

        assertThatThrownBy(() -> TypeResolver.extractMapValueType(type))
                .isInstanceOf(BeanInstantiationException.class);
    }

    // ==================== primitive/enum/wrapper skip instantiation ====================

    @Test
    void verifyAndExtractTypes_shouldSkipInstantiation_forPrimitiveType() throws Exception {
        Type type = GsonTypesUtil.getType(List.class, int.class);

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(type);

        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(int.class);
    }

    @Test
    void verifyAndExtractTypes_shouldSkipInstantiation_forEnumType() throws Exception {
        Type type = GsonTypesUtil.getType(List.class, Thread.State.class);

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(type);

        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(Thread.State.class);
    }

    @Test
    void verifyAndExtractTypes_shouldSucceed_forInstantiableComplexType() throws Exception {
        Type type = GsonTypesUtil.getType(List.class, PrimitiveFoo.class);

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(type);

        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(PrimitiveFoo.class);
    }

    // ==================== extractElementClass edge cases ====================

    @Test
    void extractElementClass_shouldReturnRawType_forParameterizedTypeWithNoArgs() throws Exception {
        // GsonTypesUtil.getType(List.class) creates a ParameterizedType - but it always has args.
        // Use a custom Type to simulate zero args edge case
        ParameterizedType zeroArgsType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[0];
            }

            @Override
            public Type getRawType() {
                return ArrayList.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Class<?> result = TypeResolver.extractElementClass(zeroArgsType);

        assertThat(result).isEqualTo(ArrayList.class);
    }

    @Test
    void extractElementClass_shouldResolveClassName_forNonClassNonParameterizedType() throws Exception {
        // Simulate a Type that is not Class or ParameterizedType
        Type customType = new Type() {
            @Override
            public String getTypeName() {
                return "java.lang.String";
            }
        };

        Class<?> result = TypeResolver.extractElementClass(customType);

        assertThat(result).isEqualTo(String.class);
    }

    // ==================== additional edge cases ====================

    @Test
    void getNestedGenericType_shouldReturnNestedParameterizedType() throws Exception {
        Type genericType = genericTypeOf("mapOfLists");

        Type valueType = TypeResolver.getNestedGenericType(genericType, 1);

        assertThat(valueType).isInstanceOf(ParameterizedType.class);
        ParameterizedType paramValueType = (ParameterizedType) valueType;
        assertThat(paramValueType.getRawType()).isEqualTo(List.class);
    }

    @Test
    void getInnermostElementType_shouldHandleCollectionOfMaps() {
        List<Map<String, Integer>> listOfMaps = List.of(Map.of("key", 42));

        Class<?> result = TypeResolver.getInnermostElementType(listOfMaps);

        assertThat(result).isEqualTo(Integer.class);
    }

    @Test
    void getInnermostElementType_shouldHandleDeepNestedCollection() {
        List<List<List<String>>> deep = List.of(List.of(List.of("deep")));

        Class<?> result = TypeResolver.getInnermostElementType(deep);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void extractCollectionElementType_shouldReturnElementType_forSetType() throws Exception {
        Type type = GsonTypesUtil.getType(Set.class, String.class);

        Class<?> result = TypeResolver.extractCollectionElementType(type);

        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void verifyAndExtractTypes_shouldHandleMapWithPrimitiveKeyAndComplexValue() throws Exception {
        Type type = genericTypeOf("intPrimitiveFooMap");

        Class<?>[] types = TypeResolver.verifyAndExtractTypes(type);

        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(Integer.class);
        assertThat(types[1]).isEqualTo(PrimitiveFoo.class);
    }
}
