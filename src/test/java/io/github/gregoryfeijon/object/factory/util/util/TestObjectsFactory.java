package io.github.gregoryfeijon.object.factory.util.util;

import io.github.gregoryfeijon.object.factory.util.domain.BarWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.FooDuplicated;
import io.github.gregoryfeijon.object.factory.util.domain.FooWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.NonSerializableObject;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectBar;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectFoo;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveArrayHolder;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveBar;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.object.factory.util.domain.VerifyValueSource;
import io.github.gregoryfeijon.object.factory.util.domain.WrapperArrayHolder;
import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestObjectsFactory {

    public static PrimitiveFoo createPrimitiveFoo() {
        return PrimitiveFoo.builder()
                .intValue(1)
                .longValue(2L)
                .boolValue(true)
                .build();
    }

    public static PrimitiveBar createPrimitiveBar() {
        return PrimitiveBar.builder()
                .iVal(1)
                .longValue(2L)
                .boolValue(true)
                .build();
    }

    public static ObjectFoo createObjectFoo() {
        return ObjectFoo.builder()
                .integerValue(10)
                .stringValue("foo")
                .bigDecimalValue(BigDecimal.valueOf(100.5))
                .build();
    }

    public static ObjectBar createObjectBar() {
        return ObjectBar.builder()
                .integerValue(10)
                .stringValue("foo")
                .bdValue(BigDecimal.valueOf(100.5))
                .build();
    }

    public static FooWrapper createFooWrapper() {
        return FooWrapper.builder()
                .primitiveFoo(createPrimitiveFoo())
                .objectFoo(createObjectFoo())
                .primitiveFooList(List.of(createPrimitiveFoo()))
                .objectFooMap(Map.of("key", createObjectFoo()))
                .build();
    }

    public static BarWrapper createBarWrapper() {
        return BarWrapper.builder()
                .primitiveBar(createPrimitiveBar())
                .objectBar(createObjectBar())
                .primitiveBarList(List.of(createPrimitiveBar()))
                .objectBarMap(Map.of("key", createObjectBar()))
                .build();
    }

    public static PrimitiveArrayHolder createPrimitiveArrayHolder() {
        return PrimitiveArrayHolder.builder()
                .intValues(new int[]{1, 2, 3})
                .build();
    }

    public static WrapperArrayHolder createWrapperArrayHolder() {
        return WrapperArrayHolder.builder()
                .integerValues(new Integer[]{1, 2, 3})
                .build();
    }

    public static NonSerializableObject createNonSerializableObject() {
        return NonSerializableObject.builder()
                .thread(new Thread())
                .build();
    }

    public static FooDuplicated createFooDuplicatedObject() {
        return FooDuplicated.builder()
                .nome1("nome 1 duplicado")
                .nome2("nome 2 duplicado")
                .build();
    }

    public static VerifyValueSource createVerifyValueSource() {
        return VerifyValueSource.builder()
                .sameType("ok")
                .wrapperToPrimitiveNull(null)
                .primitiveToWrapperZero(0)
                .status(StatusTestSource.ACTIVE)
                .listDifferentType(List.of("a", "b"))
                .fallback("stringFallback")
                .build();
    }
}
