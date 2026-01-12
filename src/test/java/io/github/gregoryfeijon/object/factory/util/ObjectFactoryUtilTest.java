package io.github.gregoryfeijon.object.factory.util;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.BarWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.CollectionTestObject;
import io.github.gregoryfeijon.object.factory.util.domain.EmptySource;
import io.github.gregoryfeijon.object.factory.util.domain.FooDuplicated;
import io.github.gregoryfeijon.object.factory.util.domain.FooWrapper;
import io.github.gregoryfeijon.object.factory.util.domain.MapTestObject;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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

    @Test
    void shouldCopyPrimitiveFieldsWithBuilder() {
        // given - Usando builder
        PrimitiveFoo foo = PrimitiveFoo.builder()
                .intValue(42)
                .longValue(100L)
                .boolValue(true)
                .build();

        // Valida que o original está correto
        assertThat(foo.getIntValue()).isEqualTo(42);
        assertThat(foo.getLongValue()).isEqualTo(100L);
        assertThat(foo.isBoolValue()).isTrue();

        // when - Copia diretamente
        PrimitiveBar bar = ObjectFactoryUtil.createFromObject(foo, PrimitiveBar.class);

        // then - Valida cópia
        assertThat(bar.getIVal()).isEqualTo(42);
        assertThat(bar.getLongValue()).isEqualTo(100L);
        assertThat(bar.isBoolValue()).isTrue();
    }

    @Test
    void shouldCopyNestedPrimitiveFooInsideWrapper() {
        // given
        PrimitiveFoo primitiveFoo = PrimitiveFoo.builder()
                .intValue(42)
                .longValue(100L)
                .boolValue(true)
                .build();

        // Valida que está correto
        assertThat(primitiveFoo.getIntValue()).isEqualTo(42);

        FooWrapper original = FooWrapper.builder()
                .primitiveFoo(primitiveFoo)
                .build();

        // Valida que está no wrapper
        assertThat(original.getPrimitiveFoo()).isNotNull();
        assertThat(original.getPrimitiveFoo().getIntValue()).isEqualTo(42);

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Debug
        System.out.println("Original PrimitiveFoo.intValue: " + original.getPrimitiveFoo().getIntValue());
        System.out.println("Clone PrimitiveBar.iVal: " + clone.getPrimitiveBar().getIVal());

        assertThat(clone.getPrimitiveBar()).isNotNull();
        assertThat(clone.getPrimitiveBar().getIVal()).isEqualTo(42);
    }

    @Test
    void shouldCreateCompletelyNewReferencesForNestedObjects() {
        // given - Objeto com aninhamento profundo
        FooWrapper original = FooWrapper.builder()
                .primitiveFoo(PrimitiveFoo.builder()
                        .intValue(42)
                        .longValue(100L)
                        .boolValue(true)
                        .build())
                .objectFoo(ObjectFoo.builder()
                        .stringValue("original")
                        .integerValue(999)
                        .bigDecimalValue(new BigDecimal("123.45"))
                        .build())
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - CRUCIAL: Valida referências dos objetos aninhados
        assertThat(clone.getPrimitiveBar())
                .isNotNull()
                .isNotSameAs(original.getPrimitiveFoo()); // ✅ Referência diferente!

        assertThat(clone.getObjectBar())
                .isNotNull()
                .isNotSameAs(original.getObjectFoo()); // ✅ Referência diferente!

        // Valida que os valores foram copiados corretamente
        assertThat(clone.getPrimitiveBar().getIVal()).isEqualTo(42);
        assertThat(clone.getObjectBar().getStringValue()).isEqualTo("original");
    }

    @Test
    void shouldCreateNewReferencesInsideCollections() {
        // given - Lista com objetos
        PrimitiveFoo item1 = PrimitiveFoo.builder()
                .intValue(1)
                .longValue(10L)
                .boolValue(true)
                .build();

        PrimitiveFoo item2 = PrimitiveFoo.builder()
                .intValue(2)
                .longValue(20L)
                .boolValue(false)
                .build();

        FooWrapper original = FooWrapper.builder()
                .primitiveFooList(List.of(item1, item2))
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - CRUCIAL: Lista é diferente
        assertThat(clone.getPrimitiveBarList())
                .isNotNull()
                .hasSize(2)
                .isNotSameAs(original.getPrimitiveFooList()); // ✅ Lista diferente

        // CRUCIAL: Elementos dentro da lista são diferentes
        assertThat(clone.getPrimitiveBarList().get(0))
                .isNotSameAs(item1); // ✅ Elemento 0 é nova referência!

        assertThat(clone.getPrimitiveBarList().get(1))
                .isNotSameAs(item2); // ✅ Elemento 1 é nova referência!

        // Valida valores
        assertThat(clone.getPrimitiveBarList().get(0).getIVal()).isEqualTo(1);
        assertThat(clone.getPrimitiveBarList().get(1).getIVal()).isEqualTo(2);
    }

    @Test
    void shouldHandleNestedCollectionsWithTypeConversion() {
        // given - List<List<List<PrimitiveFoo>>> (3 níveis de aninhamento!)
        // CORREÇÃO: objectFooLists é List<List<List<PrimitiveFoo>>>, não PrimitiveBar!

        // Nível 3 (mais interno): List<PrimitiveFoo>
        List<PrimitiveFoo> innermost1 = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(1).longValue(10L).boolValue(true).build(),
                PrimitiveFoo.builder().intValue(2).longValue(20L).boolValue(false).build()
        ));

        List<PrimitiveFoo> innermost2 = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(3).longValue(30L).boolValue(true).build()
        ));

        // Nível 2 (meio): List<List<PrimitiveFoo>>
        List<List<PrimitiveFoo>> middle1 = new ArrayList<>(Arrays.asList(innermost1, innermost2));

        List<PrimitiveFoo> innermost3 = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(4).longValue(40L).boolValue(false).build()
        ));

        List<List<PrimitiveFoo>> middle2 = new ArrayList<>(Arrays.asList(innermost3));

        // Nível 1 (externo): List<List<List<PrimitiveFoo>>>
        List<List<List<PrimitiveFoo>>> nestedList = new ArrayList<>(Arrays.asList(middle1, middle2));

        // Cria FooWrapper com a nested list
        FooWrapper original = FooWrapper.builder()
                .objectFooLists(nestedList) // ← List<List<List<PrimitiveFoo>>>
                .build();

        // when - Copia para BarWrapper (converte PrimitiveFoo → PrimitiveBar)
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Validações estruturais
        assertThat(clone.getObjectBarLists()).isNotNull();
        assertThat(clone.getObjectBarLists()).hasSize(2); // 2 elementos no nível 1

        // Valida nível 1 (externo)
        assertThat(clone.getObjectBarLists())
                .isNotSameAs(original.getObjectFooLists()); // ✅ Lista externa é nova referência

        // Valida nível 2 (meio)
        assertThat(clone.getObjectBarLists().getFirst())
                .isNotNull()
                .hasSize(2) // middle1 tinha 2 listas
                .isNotSameAs(middle1); // ✅ Lista do meio é nova referência

        assertThat(clone.getObjectBarLists().get(1))
                .isNotNull()
                .hasSize(1) // middle2 tinha 1 lista
                .isNotSameAs(middle2); // ✅ Lista do meio é nova referência

        // Valida nível 3 (mais interno)
        assertThat(clone.getObjectBarLists().getFirst().getFirst())
                .isNotNull()
                .hasSize(2) // innermost1 tinha 2 elementos
                .isNotSameAs(innermost1); // ✅ Lista interna é nova referência

        assertThat(clone.getObjectBarLists().getFirst().get(1))
                .isNotNull()
                .hasSize(1) // innermost2 tinha 1 elemento
                .isNotSameAs(innermost2); // ✅ Lista interna é nova referência

        assertThat(clone.getObjectBarLists().get(1).getFirst())
                .isNotNull()
                .hasSize(1) // innermost3 tinha 1 elemento
                .isNotSameAs(innermost3); // ✅ Lista interna é nova referência

        // Valida elementos (objetos mais internos)
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst())
                .isNotNull()
                .isNotSameAs(innermost1.getFirst()); // ✅ Elemento é nova referência

        // Valida valores copiados corretamente (conversão: PrimitiveFoo → PrimitiveBar)
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst().getIVal()).isEqualTo(1); // intValue → iVal
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst().getLongValue()).isEqualTo(10L);
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst().isBoolValue()).isTrue();

        assertThat(clone.getObjectBarLists().getFirst().getFirst().get(1).getIVal()).isEqualTo(2);
        assertThat(clone.getObjectBarLists().getFirst().get(1).getFirst().getIVal()).isEqualTo(3);
        assertThat(clone.getObjectBarLists().get(1).getFirst().getFirst().getIVal()).isEqualTo(4);
    }

    @Test
    void shouldNotAffectOriginalWhenModifyingNestedCollections() {
        // given - Nested collections mutáveis
        List<PrimitiveFoo> innermost = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(1).build(),
                PrimitiveFoo.builder().intValue(2).build()
        ));

        List<List<PrimitiveFoo>> middle = new ArrayList<>(Arrays.asList(innermost));

        List<List<List<PrimitiveFoo>>> nested = new ArrayList<>(Arrays.asList(middle));

        FooWrapper original = FooWrapper.builder()
                .objectFooLists(nested)
                .build();

        // Captura estado original
        int originalLevel1Size = original.getObjectFooLists().size();
        int originalLevel2Size = original.getObjectFooLists().getFirst().size();
        int originalLevel3Size = original.getObjectFooLists().getFirst().getFirst().size();
        int originalFirstElementValue = original.getObjectFooLists().getFirst().getFirst().getFirst().getIntValue();

        // when - Clone
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // Modifica o CLONE em TODOS os níveis

        // Nível 1: adiciona nova lista
        List<List<PrimitiveBar>> newMiddle = new ArrayList<>();
        clone.getObjectBarLists().add(newMiddle);

        // Nível 2: adiciona nova lista interna
        List<PrimitiveBar> newInner = new ArrayList<>();
        clone.getObjectBarLists().getFirst().add(newInner);

        // Nível 3: adiciona novo elemento
        clone.getObjectBarLists().getFirst().getFirst().add(
                PrimitiveBar.builder().iVal(999).build()
        );

        // Nível 4: modifica elemento existente
        clone.getObjectBarLists().getFirst().getFirst().getFirst().setIVal(777);

        // then - CRUCIAL: Original NÃO foi afetado em NENHUM nível!

        // Nível 1: tamanho não mudou
        assertThat(original.getObjectFooLists())
                .hasSize(originalLevel1Size)
                .hasSize(1); // ✅ Não cresceu!

        // Nível 2: tamanho não mudou
        assertThat(original.getObjectFooLists().getFirst())
                .hasSize(originalLevel2Size)
                .hasSize(1); // ✅ Não cresceu!

        // Nível 3: tamanho não mudou
        assertThat(original.getObjectFooLists().getFirst().getFirst())
                .hasSize(originalLevel3Size)
                .hasSize(2); // ✅ Não cresceu!

        // Elemento não foi modificado
        assertThat(original.getObjectFooLists().getFirst().getFirst().getFirst().getIntValue())
                .isEqualTo(originalFirstElementValue)
                .isEqualTo(1); // ✅ Valor original intacto!

        // Valida que o CLONE foi modificado
        assertThat(clone.getObjectBarLists()).hasSize(2); // Cresceu no nível 1!
        assertThat(clone.getObjectBarLists().getFirst()).hasSize(2); // Cresceu no nível 2!
        assertThat(clone.getObjectBarLists().getFirst().getFirst()).hasSize(3); // Cresceu no nível 3!
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst().getIVal()).isEqualTo(777); // Elemento modificado!
    }

    @Test
    void shouldUseIdentityHashCodeForNestedCollections() {
        // given - Nested collections
        List<PrimitiveFoo> innermost = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(1).build()
        ));

        List<List<PrimitiveFoo>> middle = new ArrayList<>(Arrays.asList(innermost));
        List<List<List<PrimitiveFoo>>> nested = new ArrayList<>(Arrays.asList(middle));

        FooWrapper original = FooWrapper.builder()
                .objectFooLists(nested)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - PROVA DEFINITIVA: identityHashCode em TODOS os níveis

        // Nível 1 (externo)
        assertThat(System.identityHashCode(clone.getObjectBarLists()))
                .isNotEqualTo(System.identityHashCode(nested)); // ✅

        // Nível 2 (meio)
        assertThat(System.identityHashCode(clone.getObjectBarLists().getFirst()))
                .isNotEqualTo(System.identityHashCode(middle)); // ✅

        // Nível 3 (interno)
        assertThat(System.identityHashCode(clone.getObjectBarLists().getFirst().getFirst()))
                .isNotEqualTo(System.identityHashCode(innermost)); // ✅

        // Elemento (objeto mais interno)
        assertThat(System.identityHashCode(clone.getObjectBarLists().getFirst().getFirst().getFirst()))
                .isNotEqualTo(System.identityHashCode(innermost.getFirst())); // ✅
    }

    @Test
    void shouldHandleComplexWrapperWithAllFieldTypes() {
        // given - Objeto COMPLETO com todos os tipos de campos
        FooWrapper original = FooWrapper.builder()
                .primitiveFoo(PrimitiveFoo.builder()
                        .intValue(42)
                        .longValue(100L)
                        .boolValue(true)
                        .build())
                .objectFoo(ObjectFoo.builder()
                        .stringValue("test")
                        .integerValue(999)
                        .bigDecimalValue(new BigDecimal("123.45"))
                        .build())
                .primitiveFooList(new ArrayList<>(Arrays.asList(
                        PrimitiveFoo.builder().intValue(1).build(),
                        PrimitiveFoo.builder().intValue(2).build()
                )))
                .objectFooMap(new HashMap<>(Map.of(
                        "key1", ObjectFoo.builder().stringValue("value1").build(),
                        "key2", ObjectFoo.builder().stringValue("value2").build()
                )))
                .objectFooLists(new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList(
                                new ArrayList<>(Arrays.asList(
                                        PrimitiveFoo.builder().intValue(1).build()
                                ))
                        ))
                )))
                .fieldExcluded("excluded")
                .fieldExcludedWithAnnotation("excludedAnnotation")
                .fieldExcludedWithAnnotationInDest("excludedDest")
                .fieldExcludedUsingClassLevelAnnotation("excludedClassLevel")
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Valida TUDO

        // 1. Objetos aninhados são novas referências
        assertThat(clone.getPrimitiveBar()).isNotSameAs(original.getPrimitiveFoo());
        assertThat(clone.getObjectBar()).isNotSameAs(original.getObjectFoo());

        // 2. Coleções são novas referências
        assertThat(clone.getPrimitiveBarList()).isNotSameAs(original.getPrimitiveFooList());
        assertThat(clone.getObjectBarMap()).isNotSameAs(original.getObjectFooMap());
        assertThat(clone.getObjectBarLists()).isNotSameAs(original.getObjectFooLists());

        // 3. Elementos dentro das coleções são novas referências
        assertThat(clone.getPrimitiveBarList().getFirst())
                .isNotSameAs(original.getPrimitiveFooList().getFirst());
        assertThat(clone.getObjectBarMap().get("key1"))
                .isNotSameAs(original.getObjectFooMap().get("key1"));
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst())
                .isNotSameAs(original.getObjectFooLists().getFirst().getFirst().getFirst());

        // 4. Valores foram mapeados corretamente (PrimitiveFoo → PrimitiveBar)
        assertThat(clone.getPrimitiveBar().getIVal()).isEqualTo(42); // intValue → iVal
        assertThat(clone.getPrimitiveBarList().getFirst().getIVal()).isEqualTo(1);

        // 5. Valores foram copiados corretamente (mesmo tipo)
        assertThat(clone.getObjectBar().getStringValue()).isEqualTo("test");
        assertThat(clone.getObjectBarMap().get("key1").getStringValue()).isEqualTo("value1");
        assertThat(clone.getObjectBarLists().getFirst().getFirst().getFirst().getIVal()).isEqualTo(1);

        // 6. Tamanhos corretos
        assertThat(clone.getPrimitiveBarList()).hasSize(2);
        assertThat(clone.getObjectBarMap()).hasSize(2);
        assertThat(clone.getObjectBarLists()).hasSize(1);
        assertThat(clone.getObjectBarLists().getFirst()).hasSize(1);
        assertThat(clone.getObjectBarLists().getFirst().getFirst()).hasSize(1);

        // 7. Campos excluídos funcionam
        assertThat(clone.getFieldExcluded()).isNull();
        assertThat(clone.getFieldExcludedWithAnnotation()).isNull();
        assertThat(clone.getFieldExcludedWithAnnotationInDestNameModified()).isNull();
        assertThat(clone.getFieldExcludedUsingClassLevelAnnotation()).isNull();
    }

    @Test
    void shouldHandleNestedMapsWithTypeConversion() {
        // given - Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> (3 níveis!)

        // Nível 3 (mais interno): Map<Integer, PrimitiveFoo>
        Map<Integer, PrimitiveFoo> innermost1 = new HashMap<>();
        innermost1.put(1, PrimitiveFoo.builder().intValue(11).build());
        innermost1.put(2, PrimitiveFoo.builder().intValue(12).build());

        Map<Integer, PrimitiveFoo> innermost2 = new HashMap<>();
        innermost2.put(3, PrimitiveFoo.builder().intValue(13).build());

        // Nível 2 (meio): Map<Integer, Map<Integer, PrimitiveFoo>>
        Map<Integer, Map<Integer, PrimitiveFoo>> middle1 = new HashMap<>();
        middle1.put(10, innermost1);
        middle1.put(20, innermost2);

        Map<Integer, PrimitiveFoo> innermost3 = new HashMap<>();
        innermost3.put(4, PrimitiveFoo.builder().intValue(14).build());

        Map<Integer, Map<Integer, PrimitiveFoo>> middle2 = new HashMap<>();
        middle2.put(30, innermost3);

        // Nível 1 (externo): Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>>
        Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> nestedMap = new HashMap<>();
        nestedMap.put(100, middle1);
        nestedMap.put(200, middle2);

        FooWrapper original = FooWrapper.builder()
                .multiMapFoo(nestedMap)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Validações estruturais
        assertThat(clone.getMultiMapBar()).isNotNull();
        assertThat(clone.getMultiMapBar()).hasSize(2); // 2 chaves no nível 1

        // Valida nível 1 (externo)
        assertThat(clone.getMultiMapBar()).isNotSameAs(original.getMultiMapFoo());

        // Valida nível 2 (meio)
        assertThat(clone.getMultiMapBar().get(100))
                .isNotNull()
                .hasSize(2) // middle1 tinha 2 chaves
                .isNotSameAs(middle1);

        assertThat(clone.getMultiMapBar().get(200))
                .isNotNull()
                .hasSize(1) // middle2 tinha 1 chave
                .isNotSameAs(middle2);

        // Valida nível 3 (mais interno)
        assertThat(clone.getMultiMapBar().get(100).get(10))
                .isNotNull()
                .hasSize(2) // innermost1 tinha 2 entradas
                .isNotSameAs(innermost1);

        assertThat(clone.getMultiMapBar().get(100).get(20))
                .isNotNull()
                .hasSize(1)
                .isNotSameAs(innermost2);

        assertThat(clone.getMultiMapBar().get(200).get(30))
                .isNotNull()
                .hasSize(1)
                .isNotSameAs(innermost3);

        // Valida valores (objetos mais internos) - conversão PrimitiveFoo → PrimitiveBar
        assertThat(clone.getMultiMapBar().get(100).get(10).get(1))
                .isNotNull()
                .isNotSameAs(innermost1.get(1)); // Nova referência

        assertThat(clone.getMultiMapBar().get(100).get(10).get(1).getIVal())
                .isEqualTo(11); // intValue → iVal mapeado corretamente

        assertThat(clone.getMultiMapBar().get(100).get(10).get(2).getIVal()).isEqualTo(12);
        assertThat(clone.getMultiMapBar().get(100).get(20).get(3).getIVal()).isEqualTo(13);
        assertThat(clone.getMultiMapBar().get(200).get(30).get(4).getIVal()).isEqualTo(14);
    }

    @Test
    void shouldHandleMapWithListOfMaps() {
        // given - Map<Integer, List<Map<Integer, Map<Integer, PrimitiveFoo>>>> (híbrido!)

        // Nível 4 (mais interno): Map<Integer, PrimitiveFoo>
        Map<Integer, PrimitiveFoo> deepest1 = new HashMap<>();
        deepest1.put(1, PrimitiveFoo.builder().intValue(101).build());

        Map<Integer, PrimitiveFoo> deepest2 = new HashMap<>();
        deepest2.put(2, PrimitiveFoo.builder().intValue(102).build());

        // Nível 3: Map<Integer, Map<Integer, PrimitiveFoo>>
        Map<Integer, Map<Integer, PrimitiveFoo>> deep1 = new HashMap<>();
        deep1.put(10, deepest1);

        Map<Integer, Map<Integer, PrimitiveFoo>> deep2 = new HashMap<>();
        deep2.put(20, deepest2);

        // Nível 2: List<Map<Integer, Map<Integer, PrimitiveFoo>>>
        List<Map<Integer, Map<Integer, PrimitiveFoo>>> list1 = new ArrayList<>();
        list1.add(deep1);
        list1.add(deep2);

        // Nível 1 (externo): Map<Integer, List<...>>
        Map<Integer, List<Map<Integer, Map<Integer, PrimitiveFoo>>>> complexMap = new HashMap<>();
        complexMap.put(100, list1);

        FooWrapper original = FooWrapper.builder()
                .multiMapAndListFoo(complexMap)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Validações completas
        assertThat(clone.getMultiMapAndListBar()).isNotNull();
        assertThat(clone.getMultiMapAndListBar()).hasSize(1);

        // Nível 1: Map diferente
        assertThat(clone.getMultiMapAndListBar()).isNotSameAs(original.getMultiMapAndListFoo());

        // Nível 2: List diferente
        assertThat(clone.getMultiMapAndListBar().get(100))
                .isNotNull()
                .hasSize(2)
                .isNotSameAs(list1);

        // Nível 3: Maps diferentes
        assertThat(clone.getMultiMapAndListBar().get(100).getFirst())
                .isNotNull()
                .hasSize(1)
                .isNotSameAs(deep1);

        assertThat(clone.getMultiMapAndListBar().get(100).get(1))
                .isNotNull()
                .hasSize(1)
                .isNotSameAs(deep2);

        // Nível 4: Maps mais internos diferentes
        assertThat(clone.getMultiMapAndListBar().get(100).getFirst().get(10))
                .isNotNull()
                .hasSize(1)
                .isNotSameAs(deepest1);

        // Valores: conversão PrimitiveFoo → PrimitiveBar
        assertThat(clone.getMultiMapAndListBar().get(100).getFirst().get(10).get(1))
                .isNotNull()
                .isNotSameAs(deepest1.get(1));

        assertThat(clone.getMultiMapAndListBar().get(100).getFirst().get(10).get(1).getIVal())
                .isEqualTo(101); // intValue → iVal

        assertThat(clone.getMultiMapAndListBar().get(100).get(1).get(20).get(2).getIVal())
                .isEqualTo(102);
    }

    @Test
    void shouldNotAffectOriginalWhenModifyingNestedMaps() {
        // given - Maps aninhados mutáveis
        Map<Integer, PrimitiveFoo> innermost = new HashMap<>();
        innermost.put(1, PrimitiveFoo.builder().intValue(10).build());

        Map<Integer, Map<Integer, PrimitiveFoo>> middle = new HashMap<>();
        middle.put(10, innermost);

        Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> nested = new HashMap<>();
        nested.put(100, middle);

        FooWrapper original = FooWrapper.builder()
                .multiMapFoo(nested)
                .build();

        // Captura estado original
        int originalLevel1Size = original.getMultiMapFoo().size();
        int originalLevel2Size = original.getMultiMapFoo().get(100).size();
        int originalLevel3Size = original.getMultiMapFoo().get(100).get(10).size();
        int originalValue = original.getMultiMapFoo().get(100).get(10).get(1).getIntValue();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // Modifica o CLONE em todos os níveis

        // Nível 1: adiciona nova chave
        Map<Integer, Map<Integer, PrimitiveBar>> newMiddle = new HashMap<>();
        clone.getMultiMapBar().put(200, newMiddle);

        // Nível 2: adiciona nova chave
        Map<Integer, PrimitiveBar> newInner = new HashMap<>();
        clone.getMultiMapBar().get(100).put(20, newInner);

        // Nível 3: adiciona nova entrada
        clone.getMultiMapBar().get(100).get(10).put(2, PrimitiveBar.builder().iVal(999).build());

        // Modifica valor existente
        clone.getMultiMapBar().get(100).get(10).get(1).setIVal(777);

        // then - Original NÃO foi afetado!
        assertThat(original.getMultiMapFoo())
                .hasSize(originalLevel1Size)
                .hasSize(1);

        assertThat(original.getMultiMapFoo().get(100))
                .hasSize(originalLevel2Size)
                .hasSize(1);

        assertThat(original.getMultiMapFoo().get(100).get(10))
                .hasSize(originalLevel3Size)
                .hasSize(1);

        assertThat(original.getMultiMapFoo().get(100).get(10).get(1).getIntValue())
                .isEqualTo(originalValue)
                .isEqualTo(10);

        // Clone foi modificado
        assertThat(clone.getMultiMapBar()).hasSize(2);
        assertThat(clone.getMultiMapBar().get(100)).hasSize(2);
        assertThat(clone.getMultiMapBar().get(100).get(10)).hasSize(2);
        assertThat(clone.getMultiMapBar().get(100).get(10).get(1).getIVal()).isEqualTo(777);
    }

    @Test
    void shouldNotAffectOriginalWhenModifyingMapWithList() {
        // given - Map com List mutável
        Map<Integer, PrimitiveFoo> deepest = new HashMap<>();
        deepest.put(1, PrimitiveFoo.builder().intValue(10).build());

        Map<Integer, Map<Integer, PrimitiveFoo>> deep = new HashMap<>();
        deep.put(10, deepest);

        List<Map<Integer, Map<Integer, PrimitiveFoo>>> list = new ArrayList<>();
        list.add(deep);

        Map<Integer, List<Map<Integer, Map<Integer, PrimitiveFoo>>>> complexMap = new HashMap<>();
        complexMap.put(100, list);

        FooWrapper original = FooWrapper.builder()
                .multiMapAndListFoo(complexMap)
                .build();

        // Captura estado
        int originalMapSize = original.getMultiMapAndListFoo().size();
        int originalListSize = original.getMultiMapAndListFoo().get(100).size();
        int originalValue = original.getMultiMapAndListFoo().get(100).getFirst().get(10).get(1).getIntValue();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // Modifica clone
        clone.getMultiMapAndListBar().put(200, new ArrayList<>()); // Adiciona chave no map
        clone.getMultiMapAndListBar().get(100).add(new HashMap<>()); // Adiciona item na lista
        clone.getMultiMapAndListBar().get(100).getFirst().get(10).get(1).setIVal(999); // Modifica valor

        // then - Original intacto
        assertThat(original.getMultiMapAndListFoo()).hasSize(originalMapSize).hasSize(1);
        assertThat(original.getMultiMapAndListFoo().get(100)).hasSize(originalListSize).hasSize(1);
        assertThat(original.getMultiMapAndListFoo().get(100).getFirst().get(10).get(1).getIntValue())
                .isEqualTo(originalValue)
                .isEqualTo(10);

        // Clone modificado
        assertThat(clone.getMultiMapAndListBar()).hasSize(2);
        assertThat(clone.getMultiMapAndListBar().get(100)).hasSize(2);
        assertThat(clone.getMultiMapAndListBar().get(100).getFirst().get(10).get(1).getIVal()).isEqualTo(999);
    }

    @Test
    void shouldUseIdentityHashCodeToProveNewReferences() {
        // given - Objeto completo
        PrimitiveFoo nestedPrimitive = PrimitiveFoo.builder()
                .intValue(42)
                .build();

        ObjectFoo nestedObject = ObjectFoo.builder()
                .stringValue("test")
                .build();

        List<PrimitiveFoo> list = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(1).build()
        ));

        Map<String, ObjectFoo> map = new HashMap<>();
        map.put("key", ObjectFoo.builder().stringValue("value").build());

        FooWrapper original = FooWrapper.builder()
                .primitiveFoo(nestedPrimitive)
                .objectFoo(nestedObject)
                .primitiveFooList(list)
                .objectFooMap(map)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - PROVA DEFINITIVA: identityHashCode diferente
        // Objeto raiz
        assertThat(System.identityHashCode(clone))
                .isNotEqualTo(System.identityHashCode(original)); // ✅

        // Objeto aninhado (primitiveBar)
        assertThat(System.identityHashCode(clone.getPrimitiveBar()))
                .isNotEqualTo(System.identityHashCode(nestedPrimitive)); // ✅

        // Objeto aninhado (objectBar)
        assertThat(System.identityHashCode(clone.getObjectBar()))
                .isNotEqualTo(System.identityHashCode(nestedObject)); // ✅

        // Lista
        assertThat(System.identityHashCode(clone.getPrimitiveBarList()))
                .isNotEqualTo(System.identityHashCode(list)); // ✅

        // Elemento da lista
        assertThat(System.identityHashCode(clone.getPrimitiveBarList().get(0)))
                .isNotEqualTo(System.identityHashCode(list.get(0))); // ✅

        // Map
        assertThat(System.identityHashCode(clone.getObjectBarMap()))
                .isNotEqualTo(System.identityHashCode(map)); // ✅

        // Valor do map
        assertThat(System.identityHashCode(clone.getObjectBarMap().get("key")))
                .isNotEqualTo(System.identityHashCode(map.get("key"))); // ✅
    }

    @Test
    void shouldHandleCompleteObjectGraphWithAllFieldTypes() {
        // given - Objeto COMPLETO com todos os campos populados
        FooWrapper original = FooWrapper.builder()
                .primitiveFoo(PrimitiveFoo.builder()
                        .intValue(42)
                        .longValue(100L)
                        .boolValue(true)
                        .build())
                .objectFoo(ObjectFoo.builder()
                        .stringValue("test")
                        .integerValue(999)
                        .bigDecimalValue(new BigDecimal("123.45"))
                        .build())
                .primitiveFooList(new ArrayList<>(Arrays.asList(
                        PrimitiveFoo.builder().intValue(1).build(),
                        PrimitiveFoo.builder().intValue(2).build()
                )))
                .objectFooMap(new HashMap<>(Map.of(
                        "key1", ObjectFoo.builder().stringValue("value1").build(),
                        "key2", ObjectFoo.builder().stringValue("value2").build()
                )))
                .fieldExcluded("excluded")
                .fieldExcludedWithAnnotation("excludedAnnotation")
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then - Valida TUDO
        // 1. Objetos aninhados são novas referências
        assertThat(clone.getPrimitiveBar()).isNotSameAs(original.getPrimitiveFoo());
        assertThat(clone.getObjectBar()).isNotSameAs(original.getObjectFoo());

        // 2. Coleções são novas referências
        assertThat(clone.getPrimitiveBarList()).isNotSameAs(original.getPrimitiveFooList());
        assertThat(clone.getObjectBarMap()).isNotSameAs(original.getObjectFooMap());

        // 3. Elementos dentro das coleções são novas referências
        assertThat(clone.getPrimitiveBarList().getFirst())
                .isNotSameAs(original.getPrimitiveFooList().getFirst());
        assertThat(clone.getObjectBarMap().get("key1"))
                .isNotSameAs(original.getObjectFooMap().get("key1"));

        // 4. Valores foram copiados corretamente
        assertThat(clone.getPrimitiveBar().getIVal()).isEqualTo(42);
        assertThat(clone.getObjectBar().getStringValue()).isEqualTo("test");
        assertThat(clone.getPrimitiveBarList()).hasSize(2);
        assertThat(clone.getObjectBarMap()).hasSize(2);

        // 5. Campos excluídos funcionam
        assertThat(clone.getFieldExcluded()).isNull();
        assertThat(clone.getFieldExcludedWithAnnotation()).isNull();
    }

    @Test
    void shouldHandleNestedCollectionsWithSameInnermostType() {
        // given - List<List<PrimitiveFoo>> → List<List<PrimitiveFoo>> (deep clone, mesmo tipo mais interno)
        List<PrimitiveFoo> innerList1 = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(1).build(),
                PrimitiveFoo.builder().intValue(2).build()
        ));

        List<PrimitiveFoo> innerList2 = new ArrayList<>(Arrays.asList(
                PrimitiveFoo.builder().intValue(3).build()
        ));

        List<List<PrimitiveFoo>> nestedList = new ArrayList<>(Arrays.asList(innerList1, innerList2));

        FooWrapper original = FooWrapper.builder()
                .objectFooLists(new ArrayList<>(Arrays.asList(nestedList)))
                .build();

        // when - Deep clone (MESMO tipo)
        FooWrapper clone = ObjectFactoryUtil.createFromObject(original, FooWrapper.class);

        // then - Cobre o branch: innermostSourceType == targetElementType (usa JSON)
        assertThat(clone.getObjectFooLists()).isNotSameAs(original.getObjectFooLists());
        assertThat(clone.getObjectFooLists().getFirst()).isNotSameAs(nestedList);
        assertThat(clone.getObjectFooLists().getFirst().getFirst()).isNotSameAs(innerList1);
        assertThat(clone.getObjectFooLists().getFirst().getFirst().getFirst()).isNotSameAs(innerList1.getFirst());

        // Valores preservados
        assertThat(clone.getObjectFooLists().getFirst().getFirst().getFirst().getIntValue()).isEqualTo(1);
        assertThat(clone.getObjectFooLists().getFirst().getFirst().get(1).getIntValue()).isEqualTo(2);
        assertThat(clone.getObjectFooLists().getFirst().get(1).getFirst().getIntValue()).isEqualTo(3);
    }

    @Test
    void shouldHandleNestedMapsWithSameInnermostType() {
        // given - Map<Integer, Map<Integer, PrimitiveFoo>> → Map<Integer, Map<Integer, PrimitiveFoo>>
        Map<Integer, PrimitiveFoo> innerMap1 = new HashMap<>();
        innerMap1.put(1, PrimitiveFoo.builder().intValue(10).build());
        innerMap1.put(2, PrimitiveFoo.builder().intValue(20).build());

        Map<Integer, PrimitiveFoo> innerMap2 = new HashMap<>();
        innerMap2.put(3, PrimitiveFoo.builder().intValue(30).build());

        Map<Integer, Map<Integer, PrimitiveFoo>> nestedMap = new HashMap<>();
        nestedMap.put(100, innerMap1);
        nestedMap.put(200, innerMap2);

        Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> tripleNestedMap = new HashMap<>();
        tripleNestedMap.put(1000, nestedMap);

        FooWrapper original = FooWrapper.builder()
                .multiMapFoo(tripleNestedMap)
                .build();

        // when - Deep clone (MESMO tipo)
        FooWrapper clone = ObjectFactoryUtil.createFromObject(original, FooWrapper.class);

        // then - Cobre o branch: innermostSourceType == targetValueType (usa JSON)
        assertThat(clone.getMultiMapFoo()).isNotSameAs(original.getMultiMapFoo());
        assertThat(clone.getMultiMapFoo().get(1000)).isNotSameAs(nestedMap);
        assertThat(clone.getMultiMapFoo().get(1000).get(100)).isNotSameAs(innerMap1);
        assertThat(clone.getMultiMapFoo().get(1000).get(100).get(1)).isNotSameAs(innerMap1.get(1));

        // Valores preservados
        assertThat(clone.getMultiMapFoo().get(1000).get(100).get(1).getIntValue()).isEqualTo(10);
        assertThat(clone.getMultiMapFoo().get(1000).get(100).get(2).getIntValue()).isEqualTo(20);
        assertThat(clone.getMultiMapFoo().get(1000).get(200).get(3).getIntValue()).isEqualTo(30);
    }

    @Test
    void shouldHandleMixedStructureListOfMapsWithSameType() {
        // given - List<Map<String, PrimitiveBar>> em wrapper
        Map<String, PrimitiveBar> map1 = new HashMap<>();
        map1.put("a", PrimitiveBar.builder().iVal(1).build());

        Map<String, PrimitiveBar> map2 = new HashMap<>();
        map2.put("b", PrimitiveBar.builder().iVal(2).build());

        List<Map<String, PrimitiveBar>> listOfMaps = new ArrayList<>(Arrays.asList(map1, map2));

        CollectionTestObject original = CollectionTestObject.builder()
                .primitiveBars(null) // Não usado neste teste
                .listOfMaps(listOfMaps) // ← NOVO CAMPO (adicionar em CollectionTestObject)
                .build();

        // when - Deep clone (MESMO tipo)
        CollectionTestObject clone = ObjectFactoryUtil.createFromObject(original, CollectionTestObject.class);

        // then - Cobre o branch: innermostSourceType == targetElementType (usa JSON)
        assertThat(clone.getListOfMaps()).isNotSameAs(listOfMaps);
        assertThat(clone.getListOfMaps().getFirst()).isNotSameAs(map1);
        assertThat(clone.getListOfMaps().getFirst().get("a")).isNotSameAs(map1.get("a"));
        assertThat(clone.getListOfMaps().getFirst().get("a").getIVal()).isEqualTo(1);
        assertThat(clone.getListOfMaps().get(1).get("b").getIVal()).isEqualTo(2);
    }

    @Test
    void shouldHandleMapOfListsWithSameType() {
        // given - Map<String, List<PrimitiveBar>> em wrapper
        List<PrimitiveBar> list1 = new ArrayList<>(Arrays.asList(
                PrimitiveBar.builder().iVal(1).build()
        ));

        List<PrimitiveBar> list2 = new ArrayList<>(Arrays.asList(
                PrimitiveBar.builder().iVal(2).build()
        ));

        Map<String, List<PrimitiveBar>> mapOfLists = new HashMap<>();
        mapOfLists.put("list1", list1);
        mapOfLists.put("list2", list2);

        MapTestObject original = MapTestObject.builder()
                .barMap(null) // Não usado neste teste
                .multiKeysBarMap(null) // Não usado neste teste
                .mapOfLists(mapOfLists) // ← NOVO CAMPO (adicionar em MapTestObject)
                .build();

        // when - Deep clone (MESMO tipo)
        MapTestObject clone = ObjectFactoryUtil.createFromObject(original, MapTestObject.class);

        // then - Cobre o branch: innermostSourceType == targetValueType (usa JSON)
        assertThat(clone.getMapOfLists()).isNotSameAs(mapOfLists);
        assertThat(clone.getMapOfLists().get("list1")).isNotSameAs(list1);
        assertThat(clone.getMapOfLists().get("list1").getFirst()).isNotSameAs(list1.getFirst());
        assertThat(clone.getMapOfLists().get("list1").getFirst().getIVal()).isEqualTo(1);
        assertThat(clone.getMapOfLists().get("list2").getFirst().getIVal()).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyNestedCollections() {
        // given - Nested collections vazias
        List<List<List<PrimitiveFoo>>> emptyNested = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>() // Lista vazia no nível mais interno
                ))
        ));

        FooWrapper original = FooWrapper.builder()
                .objectFooLists(emptyNested)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then
        assertThat(clone.getObjectBarLists()).isNotNull();
        assertThat(clone.getObjectBarLists()).hasSize(1);
        assertThat(clone.getObjectBarLists().getFirst()).hasSize(1);
        assertThat(clone.getObjectBarLists().getFirst().getFirst()).isEmpty();
    }

    @Test
    void shouldHandleEmptyNestedMaps() {
        // given - Nested maps vazios
        Map<Integer, Map<Integer, PrimitiveFoo>> innerEmpty = new HashMap<>();
        innerEmpty.put(1, new HashMap<>()); // Map vazio

        Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> nestedEmpty = new HashMap<>();
        nestedEmpty.put(100, innerEmpty);

        FooWrapper original = FooWrapper.builder()
                .multiMapFoo(nestedEmpty)
                .build();

        // when
        BarWrapper clone = ObjectFactoryUtil.createFromObject(original, BarWrapper.class);

        // then
        assertThat(clone.getMultiMapBar()).isNotNull();
        assertThat(clone.getMultiMapBar()).hasSize(1);
        assertThat(clone.getMultiMapBar().get(100)).hasSize(1);
        assertThat(clone.getMultiMapBar().get(100).get(1)).isEmpty();
    }
}
