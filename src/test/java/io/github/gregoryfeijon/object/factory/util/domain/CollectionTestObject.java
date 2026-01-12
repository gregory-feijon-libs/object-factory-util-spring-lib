package io.github.gregoryfeijon.object.factory.util.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 12/01/2026 às 14:34
 *
 * @author gregory.feijon
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionTestObject {

    private List<PrimitiveBar> primitiveBars;
    private List<List<List<List<PrimitiveBar>>>> primitiveMultiBars;
    private List<Map<String, PrimitiveBar>> listOfMaps;
}
