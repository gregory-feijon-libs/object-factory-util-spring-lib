package io.github.gregoryfeijon.object.factory.util.domain;

import io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ObjectCopyExclusions(value = {"alternativeName", "attributeNameDoesntExists"})
public class FooWrapper {

    @FieldCopyName(value = "primitiveBar")
    private PrimitiveFoo primitiveFoo;

    @FieldCopyName(value = "objectBar")
    private ObjectFoo objectFoo;

    @FieldCopyName(value = "primitiveBarList")
    private List<PrimitiveFoo> primitiveFooList;

    @FieldCopyName(value = "objectBarMap")
    private Map<String, ObjectFoo> objectFooMap;

    @FieldCopyName(value = "objectBarLists")
    private List<List<List<PrimitiveFoo>>> objectFooLists;

    @FieldCopyName(value = "multiMapBar")
    private Map<Integer, Map<Integer, Map<Integer, PrimitiveFoo>>> multiMapFoo;

    @FieldCopyName(value = "multiMapAndListBar")
    private Map<Integer, List<Map<Integer, Map<Integer, PrimitiveFoo>>>> multiMapAndListFoo;

    private String fieldExcluded;

    @ObjectCopyExclude
    private String fieldExcludedWithAnnotation;

    private String fieldExcludedWithAnnotationInDest;

    @FieldCopyName("alternativeName")
    private String fieldExcludedUsingClassLevelAnnotation;
}