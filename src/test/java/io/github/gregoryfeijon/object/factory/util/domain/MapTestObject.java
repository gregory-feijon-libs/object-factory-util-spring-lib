package io.github.gregoryfeijon.object.factory.util.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 12/01/2026 às 14:35
 *
 * @author gregory.feijon
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapTestObject {

    private Map<Integer, PrimitiveBar> barMap;
    private Map<Integer, Map<Integer, Map<Integer, PrimitiveBar>>> multiKeysBarMap;
    private Map<String, List<PrimitiveBar>> mapOfLists;
}
