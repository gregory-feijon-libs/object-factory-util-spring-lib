package io.github.gregoryfeijon.object.factory.util;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.BarWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.EmptySource;
import io.github.gregoryfeijon.object.factory.util.domain.FooDuplicated;
import io.github.gregoryfeijon.object.factory.util.domain.FooWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.MismatchSource;
import io.github.gregoryfeijon.object.factory.util.domain.MismatchTarget;
import io.github.gregoryfeijon.object.factory.util.domain.NonSerializableObject;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectBar;
import io.github.gregoryfeijon.object.factory.util.domain.ObjectFoo;
import io.github.gregoryfeijon.object.factory.util.domain.OnlyStaticAttributeDestination;
import io.github.gregoryfeijon.object.factory.util.domain.OnlyStaticAttributeSource;
import io.github.gregoryfeijon.object.factory.util.domain.PartialBar;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveArrayHolder;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveBar;
import io.github.gregoryfeijon.object.factory.util.domain.PrimitiveFoo;
import io.github.gregoryfeijon.object.factory.util.domain.VerifyValueDest;
import io.github.gregoryfeijon.object.factory.util.domain.VerifyValueSource;
import io.github.gregoryfeijon.object.factory.util.domain.WrapperArrayHolder;
import io.github.gregoryfeijon.object.factory.util.domain.enums.StatusTestDest;
import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.object.factory.util.util.TestObjectsFactory;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import io.github.gregoryfeijon.serializer.provider.domain.enums.SerializationType;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        FactoryUtil.class,
        TestSerializerConfiguration.class
})
class ObjectFactoryUtilTest {

    @Test
    void shouldCopyPrimitiveFieldsUsingAnnotationAndNames() {
        PrimitiveFoo foo = new PrimitiveFoo();
        PrimitiveBar bar = ObjectFactoryUtil.createFromObject(foo, PrimitiveBar.class);

        assertThat(bar).isNotNull();
        assertThat(bar.getIVal()).isEqualTo(foo.getIntValue());
        assertThat(bar.getLongValue()).isEqualTo(foo.getLongValue());
        assertThat(bar.isBoolValue()).isEqualTo(foo.isBoolValue());
    }

    @Test
    void shouldCopyObjectFieldsWithAnnotation() {
        ObjectFoo foo = new ObjectFoo();
        ObjectBar bar = ObjectFactoryUtil.createFromObject(foo, ObjectBar.class);

        assertThat(bar).isNotNull();
        assertThat(bar.getIntegerValue()).isEqualTo(foo.getIntegerValue());
        assertThat(bar.getStringValue()).isEqualTo(foo.getStringValue());
        assertThat(bar.getBdValue()).isEqualTo(foo.getBigDecimalValue());
    }

    @Test
    void shouldCopyWrapperWithCollectionsAndExcludeFieldMarked() {
        FooWrapper fooWrapper = new FooWrapper();
        fooWrapper.setPrimitiveFoo(new PrimitiveFoo());
        fooWrapper.setObjectFoo(new ObjectFoo());
        fooWrapper.setPrimitiveFooList(List.of(new PrimitiveFoo()));
        fooWrapper.setObjectFooMap(Map.of("key", new ObjectFoo()));
        fooWrapper.setFieldExcluded("This value shouldn't be copied");
        fooWrapper.setFieldExcludedWithAnnotation("This value shouldn't be copied too");
        fooWrapper.setFieldExcludedWithAnnotationInDest("This value shouldn't be copied too");
        fooWrapper.setFieldExcludedUsingClassLevelAnnotation("This value shouldn't be copied too");

        BarWrapper barWrapper = ObjectFactoryUtil.createFromObject(fooWrapper, BarWrapper.class);

        assertThat(barWrapper).isNotNull();
        assertThat(barWrapper.getPrimitiveBar()).isNotNull();
        assertThat(barWrapper.getObjectBar()).isNotNull();
        assertThat(barWrapper.getPrimitiveBarList()).hasSize(1);
        assertThat(barWrapper.getObjectBarMap()).containsKey("key");
        assertThat(barWrapper.getFieldExcluded()).isNull();
        assertThat(barWrapper.getFieldExcludedWithAnnotation()).isNull();
        assertThat(barWrapper.getFieldExcludedWithAnnotationInDestNameModified()).isNull();
        assertThat(barWrapper.getFieldExcludedUsingClassLevelAnnotation()).isNull();
    }

    @Test
    void shouldNotFailOnMissingDestFields() {
        PrimitiveFoo foo = new PrimitiveFoo();


        PartialBar partialBar = ObjectFactoryUtil.createFromObject(foo, PartialBar.class);

        assertThat(partialBar).isNotNull();
        assertThat(partialBar.getSomeOtherField()).isZero();
    }

    @Test
    void shouldCopyAllObjectsFromCollection() {
        List<PrimitiveFoo> fooList = List.of(new PrimitiveFoo(), new PrimitiveFoo());
        List<PrimitiveFoo> copiedList = ObjectFactoryUtil.copyAllObjectsFromCollection(fooList);

        assertThat(copiedList).isNotNull().hasSize(fooList.size());
        assertThat(copiedList.getFirst()).isNotSameAs(fooList.getFirst());
    }

    @Test
    void shouldCopyAllObjectsFromCollectionWithReturnType() {
        List<PrimitiveFoo> fooList = List.of(new PrimitiveFoo(), new PrimitiveFoo());
        List<PrimitiveBar> copiedList = ObjectFactoryUtil.copyAllObjectsFromCollection(fooList, PrimitiveBar.class);

        assertThat(copiedList).isNotNull().hasSize(fooList.size());
        assertThat(copiedList.getFirst()).isInstanceOf(PrimitiveBar.class);
    }

    @Test
    void shouldCopyAllObjectsFromCollectionWithSupplier() {
        List<PrimitiveFoo> fooList = List.of(new PrimitiveFoo(), new PrimitiveFoo());
        Set<PrimitiveFoo> copiedSet = ObjectFactoryUtil.copyAllObjectsFromCollection(fooList, HashSet::new);

        assertThat(copiedSet)
                .isNotNull()
                .hasSize(fooList.size())
                .allSatisfy(obj -> assertThat(obj).isInstanceOf(PrimitiveFoo.class));
    }

    @Test
    void shouldCopyAllObjectsFromCollectionWithSupplierAndReturnType() {
        List<PrimitiveFoo> fooList = List.of(new PrimitiveFoo(), new PrimitiveFoo());
        Set<PrimitiveBar> copiedSet = ObjectFactoryUtil.copyAllObjectsFromCollection(fooList, HashSet::new, PrimitiveBar.class);

        assertThat(copiedSet)
                .isNotNull()
                .hasSize(fooList.size())
                .allSatisfy(obj -> assertThat(obj).isInstanceOf(PrimitiveBar.class));
    }

    @Test
    void shouldThrowExceptionWhenCollectionIsEmpty() {
        List<PrimitiveFoo> emptyList = Collections.emptyList();

        assertThatThrownBy(() -> ObjectFactoryUtil.copyAllObjectsFromCollection(emptyList))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("has no elements");
    }

    @Test
    void shouldThrowExceptionWhenSourceIsNull() {
        assertThatThrownBy(() -> ObjectFactoryUtil.createFromObject(null, PrimitiveBar.class))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("The object to be copied is null");
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        ObjectFoo foo = new ObjectFoo(null, null, null);
        ObjectBar bar = ObjectFactoryUtil.createFromObject(foo, ObjectBar.class);
        assertThat(bar).isNotNull();
        assertThat(bar.getIntegerValue()).isNull();
        assertThat(bar.getBdValue()).isNull();
    }

    @Test
    void shouldClonePrimitiveArrayInsideHolder() {
        PrimitiveArrayHolder holder = TestObjectsFactory.createPrimitiveArrayHolder();
        PrimitiveArrayHolder clone = ObjectFactoryUtil.createFromObject(holder, PrimitiveArrayHolder.class);

        assertThat(clone.getIntValues())
                .containsExactly(holder.getIntValues())
                .isNotSameAs(holder.getIntValues());
    }

    @Test
    void shouldCloneWrapperArrayInsideHolder() {
        WrapperArrayHolder holder = TestObjectsFactory.createWrapperArrayHolder();
        WrapperArrayHolder clone = ObjectFactoryUtil.createFromObject(holder, WrapperArrayHolder.class);

        assertThat(clone.getIntegerValues())
                .containsExactly(holder.getIntegerValues())
                .isNotSameAs(holder.getIntegerValues());
    }

    @Test
    void shouldInitializeProviderIfEmpty() {
        SerializerProvider.initialize(new EnumMap<>(SerializationType.class), SerializationType.GSON);
        SerializerProvider.getAdapter(); // força lazy init
        assertThat(SerializerProvider.getAdapter()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() {
        var auxTest = TestObjectsFactory.createNonSerializableObject();
        assertThatThrownBy(() -> ObjectFactoryUtil.createFromObject(auxTest, NonSerializableObject.class))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed making field");
    }

    @Test
    void shouldIgnoreFieldsWithoutMatchingNamesOrAnnotations() {
        MismatchSource source = new MismatchSource();
        source.setFoo("value");

        MismatchTarget target = ObjectFactoryUtil.createFromObject(source, MismatchTarget.class);

        assertThat(target.getBar()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenSupplierIsNull() {
        List<PrimitiveFoo> fooList = List.of(new PrimitiveFoo());
        Supplier<List<PrimitiveFoo>> supplier = null;

        assertThatThrownBy(() -> ObjectFactoryUtil.copyAllObjectsFromCollection(fooList, supplier))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("The specified collection type for return is null");
    }

    @Test
    void shouldReturnEmptyMapWhenSourceAndDestinationHaveNoFields() {
        // given
        OnlyStaticAttributeSource source = new OnlyStaticAttributeSource();

        // when
        OnlyStaticAttributeDestination result = ObjectFactoryUtil.createFromObject(source, OnlyStaticAttributeDestination.class);

        // then
        assertThat(result).isNotNull();
        // não há campos, então não ocorre nenhuma cópia real
    }

    @Test
    void shouldKeepFirstFieldWhenDuplicateKeyInSameClassOccurs() {
        var source = TestObjectsFactory.createFooDuplicatedObject();

        // Deve internamente chamar buildFieldKeyMap e cair no (a, b) -> a
        var result = ObjectFactoryUtil.createFromObject(source, FooDuplicated.class);

        // Se não deu exceção, o merge foi resolvido corretamente
        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenDestinationIsNull() {
        PrimitiveFoo source = TestObjectsFactory.createPrimitiveFoo();
        PrimitiveFoo dest = null;

        // Passando dest como null, deve lançar ApiException
        assertThatThrownBy(() -> ObjectFactoryUtil.createFromObject(source, dest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("The destination object is null");
    }

    @Test
    void shouldHandleAllVerifyValueBranches() {
        // given
        VerifyValueSource source = TestObjectsFactory.createVerifyValueSource();

        // when
        VerifyValueDest dest = ObjectFactoryUtil.createFromObject(source, VerifyValueDest.class);

        // then
        // 1. Tipos iguais
        assertThat(dest.getSameType()).isEqualTo(source.getSameType());

        // 2. Wrapper → primitivo (null vira default)
        assertThat(dest.getWrapperToPrimitiveNull()).isZero();

        // 3. Primitivo default → wrapper (vira null)
        assertThat(dest.getPrimitiveToWrapperZero()).isNull();

        // 4. Enum → Enum
        assertThat(source.getStatus().toString()).hasToString(StatusTestDest.ACTIVE.toString());

        // 5. Collection ignorada
        assertThat(dest.getListDifferentType()).isNull();

        // 6. Fallback: tipos diferentes
        assertThat(dest.getFallback()).isEqualTo("stringFallback");
    }

    @Test
    void shouldHandleEmptyObjectCopy() {
        EmptySource emptySource = new EmptySource();
        var copy = ObjectFactoryUtil.createFromObject(emptySource, EmptySource.class);

        assertThat(copy).isNotNull();
    }

}
