package io.github.gregoryfeijon.object.factory.util.domain;

import io.github.gregoryfeijon.object.factory.util.domain.annotation.FieldCopyName;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectConstructor;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
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
@ObjectConstructor(exclude = {"fieldExcluded"})
public class BarWrapper {

    private PrimitiveBar primitiveBar;
    private ObjectBar objectBar;
    private List<PrimitiveBar> primitiveBarList;
    private Map<String, ObjectBar> objectBarMap;
    private List<List<List<PrimitiveBar>>> objectBarLists;
    private Map<Integer, Map<Integer, Map<Integer, PrimitiveBar>>> multiMapBar;
    private Map<Integer, List<Map<Integer, Map<Integer, PrimitiveBar>>>> multiMapAndListBar;
    private String fieldExcluded;
    private String fieldExcludedWithAnnotation;

    @ObjectCopyExclude
    @FieldCopyName("fieldExcludedWithAnnotationInDest")
    private String fieldExcludedWithAnnotationInDestNameModified;

    @FieldCopyName("alternativeName")
    private String fieldExcludedUsingClassLevelAnnotation;
}