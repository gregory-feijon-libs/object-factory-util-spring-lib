package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectBar;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectFoo;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        FactoryUtil.class,
        TestSerializerConfiguration.class
})
class ObjectClonerTest {

    @Test
    void serializingClone_shouldReturnNull_whenSourceValueIsNull() {
        Object result = ObjectCloner.serializingClone(null, String.class);

        assertThat(result).isNull();
    }

    @Test
    void serializingClone_shouldCloneNonNullValue() {
        String original = "test";
        Object result = ObjectCloner.serializingClone(original, String.class);

        assertThat(result)
                .isEqualTo("test")
                .isNotSameAs(original);
    }

    @Test
    void serializingCloneObjects_shouldReturnNull_whenSourceValueIsNull() {
        Object result = ObjectCloner.serializingCloneObjects(null, String.class);

        assertThat(result).isNull();
    }

    @Test
    void serializingCloneObjects_shouldUseBinarySerialization_forSimpleType() {
        Integer original = 42;
        Object result = ObjectCloner.serializingCloneObjects(original, int.class);

        assertThat(result).isEqualTo(42);
    }

    @Test
    void serializingCloneObjects_shouldUseBinarySerialization_forWrapperType() {
        Integer original = 42;
        Object result = ObjectCloner.serializingCloneObjects(original, Integer.class);

        assertThat(result).isEqualTo(42);
    }

    @Test
    void serializingCloneObjects_shouldUseJsonSerialization_forSameType() {
        ObjectFoo original = ObjectFoo.builder()
                .integerValue(10)
                .stringValue("test")
                .bigDecimalValue(BigDecimal.TEN)
                .build();

        Object result = ObjectCloner.serializingCloneObjects(original, ObjectFoo.class);

        assertThat(result)
                .isInstanceOf(ObjectFoo.class)
                .isNotSameAs(original);
        ObjectFoo cloned = (ObjectFoo) result;
        assertThat(cloned.getIntegerValue()).isEqualTo(10);
        assertThat(cloned.getStringValue()).isEqualTo("test");
    }

    @Test
    void serializingCloneObjects_shouldUseCreateFromObject_forDifferentType() {
        ObjectFoo original = ObjectFoo.builder()
                .integerValue(10)
                .stringValue("test")
                .bigDecimalValue(BigDecimal.TEN)
                .build();

        Object result = ObjectCloner.serializingCloneObjects(original, ObjectBar.class);

        assertThat(result).isInstanceOf(ObjectBar.class);
        ObjectBar bar = (ObjectBar) result;
        assertThat(bar.getIntegerValue()).isEqualTo(10);
        assertThat(bar.getStringValue()).isEqualTo("test");
        assertThat(bar.getBdValue()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void cloneToBinaryFormat_shouldCloneSerializableValue() {
        String original = "binary test";
        Object result = ObjectCloner.cloneToBinaryFormat(original);

        assertThat(result)
                .isEqualTo("binary test")
                .isNotSameAs(original);
    }

    @Test
    void cloneToJsonFormat_shouldCloneViaJson() {
        PrimitiveFoo original = PrimitiveFoo.builder()
                .intValue(42)
                .longValue(100L)
                .boolValue(true)
                .build();

        Object result = ObjectCloner.cloneToJsonFormat(original, PrimitiveFoo.class);

        assertThat(result)
                .isInstanceOf(PrimitiveFoo.class)
                .isNotSameAs(original);
        PrimitiveFoo cloned = (PrimitiveFoo) result;
        assertThat(cloned.getIntValue()).isEqualTo(42);
        assertThat(cloned.getLongValue()).isEqualTo(100L);
        assertThat(cloned.isBoolValue()).isTrue();
    }

    @Test
    void getSerializer_shouldReturnNonNullAdapter() {
        SerializerAdapter adapter = ObjectCloner.getSerializer();

        assertThat(adapter).isNotNull();
    }

    @Test
    void serializingCloneObjects_shouldHandleBigDecimalAsBinarySerialization() {
        BigDecimal original = new BigDecimal("123.45");
        Object result = ObjectCloner.serializingCloneObjects(original, BigDecimal.class);

        assertThat(result).isEqualTo(new BigDecimal("123.45"));
    }
}
