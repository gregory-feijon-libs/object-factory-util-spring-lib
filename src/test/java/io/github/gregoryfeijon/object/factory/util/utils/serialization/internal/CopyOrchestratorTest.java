package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestDest;
import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestSource;
import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        FactoryUtil.class,
        TestSerializerConfiguration.class
})
class CopyOrchestratorTest {

    // Test domain classes for CopyOrchestrator-specific tests
    @SuppressWarnings("unused")
    static class SameTypeSource {
        String name = "test";
        int primitiveValue = 42;
        Integer wrapperValue = null;
        int primitiveDefault = 0;
        StatusTestSource status = StatusTestSource.ACTIVE;
        List<String> collection = List.of("a");
        String fallbackField = "fallback";
    }

    @SuppressWarnings("unused")
    static class SameTypeDest {
        String name;
        int primitiveValue;
        int wrapperValue; // Integer -> int (wrapper to primitive, null source)
        Integer primitiveDefault; // int -> Integer (primitive to wrapper, default value)
        StatusTestDest status;
        java.util.Set<Integer> collection; // different collection type
        Object fallbackField; // different type
    }

    @SuppressWarnings("unused")
    static class PrimitiveSource {
        int value = 5;
    }

    @SuppressWarnings("unused")
    static class PrimitiveDest {
        int value;
    }

    @SuppressWarnings("unused")
    static class WrapperSource {
        String stringValue = "hello";
    }

    @SuppressWarnings("unused")
    static class WrapperDest {
        String stringValue;
    }

    private Field sourceField(Class<?> clazz, String name) throws NoSuchFieldException {
        return clazz.getDeclaredField(name);
    }

    @Test
    void verifyValue_shouldReturnCopiedValue_whenSameType() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "name");
        Field df = sourceField(SameTypeDest.class, "name");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isEqualTo("test");
    }

    @Test
    void verifyValue_shouldReturnDefaultValue_whenWrapperToPrimitiveAndNull() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "wrapperValue");
        Field df = sourceField(SameTypeDest.class, "wrapperValue");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isEqualTo(0); // default for int
    }

    @Test
    void verifyValue_shouldReturnNull_whenPrimitiveToWrapperAndDefaultValue() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "primitiveDefault");
        Field df = sourceField(SameTypeDest.class, "primitiveDefault");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isNull();
    }

    @Test
    void verifyValue_shouldConvertEnum() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "status");
        Field df = sourceField(SameTypeDest.class, "status");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isEqualTo(StatusTestDest.ACTIVE);
    }

    @Test
    void verifyValue_shouldReturnNull_whenCollectionTypeMismatch() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "collection");
        Field df = sourceField(SameTypeDest.class, "collection");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isNull();
    }

    @Test
    void copyValue_shouldReturnSourceValue_forPrimitiveType() throws Exception {
        Field sf = sourceField(PrimitiveSource.class, "value");
        Field df = sourceField(PrimitiveDest.class, "value");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.copyValue(sf, df, 5);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void copyValue_shouldCloneWrapper_forWrapperType() throws Exception {
        Field sf = sourceField(WrapperSource.class, "stringValue");
        Field df = sourceField(WrapperDest.class, "stringValue");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.copyValue(sf, df, "hello");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void copyValue_shouldHandleNullSourceValue_forWrapperType() throws Exception {
        Field sf = sourceField(WrapperSource.class, "stringValue");
        Field df = sourceField(WrapperDest.class, "stringValue");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.copyValue(sf, df, null);

        assertThat(result).isNull();
    }

    @Test
    void verifyValue_shouldFallbackToCopyValue_whenDifferentNonSpecialTypes() throws Exception {
        SameTypeSource source = new SameTypeSource();
        Field sf = sourceField(SameTypeSource.class, "fallbackField");
        Field df = sourceField(SameTypeDest.class, "fallbackField");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.verifyValue(sf, df, source);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void copyValue_shouldCloneObject_viaSerialization() throws Exception {
        PrimitiveFoo original = PrimitiveFoo.builder()
                .intValue(42)
                .longValue(100L)
                .boolValue(true)
                .build();

        // Use a field whose type is PrimitiveFoo to trigger the "else" branch (serializingCloneObjects)
        Field sf = CopyOrchestratorObjectHolder.class.getDeclaredField("fooSource");
        Field df = CopyOrchestratorObjectHolder.class.getDeclaredField("fooDest");
        sf.setAccessible(true);
        df.setAccessible(true);

        Object result = CopyOrchestrator.copyValue(sf, df, original);

        assertThat(result)
                .isInstanceOf(PrimitiveFoo.class)
                .isNotSameAs(original);
        assertThat(((PrimitiveFoo) result).getIntValue()).isEqualTo(42);
    }

    @Test
    void copyValue_shouldThrowApiException_whenSerializationFails() throws Exception {
        Field sf = CopyOrchestratorObjectHolder.class.getDeclaredField("threadSource");
        Field df = CopyOrchestratorObjectHolder.class.getDeclaredField("threadDest");
        sf.setAccessible(true);
        df.setAccessible(true);

        assertThatThrownBy(() -> CopyOrchestrator.copyValue(sf, df, new Thread()))
                .isInstanceOf(ApiException.class);
    }

    @SuppressWarnings("unused")
    static class CopyOrchestratorObjectHolder {
        PrimitiveFoo fooSource;
        PrimitiveFoo fooDest;
        Thread threadSource;
        Thread threadDest;
    }
}
