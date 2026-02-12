package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateProxyHandlerTest {

    @Test
    void unproxyValueIfNeeded_shouldReturnSameObject_whenNotProxyOrCollection() {
        String value = "test";
        Object result = HibernateProxyHandler.unproxyValueIfNeeded(value);

        assertThat(result).isSameAs(value);
    }

    @Test
    void unproxyValueIfNeeded_shouldReturnNull_whenNull() {
        Object result = HibernateProxyHandler.unproxyValueIfNeeded(null);

        assertThat(result).isNull();
    }

    @Test
    void unproxyValueIfNeeded_shouldReturnSameCollection_whenNoProxies() {
        List<String> list = List.of("a", "b", "c");
        Object result = HibernateProxyHandler.unproxyValueIfNeeded(list);

        assertThat(result).isSameAs(list);
    }

    @Test
    void unproxyValueIfNeeded_shouldReturnSameMap_whenNoProxies() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        Object result = HibernateProxyHandler.unproxyValueIfNeeded(map);

        assertThat(result).isSameAs(map);
    }

    @Test
    void unproxyValueIfNeeded_shouldReturnSameSet_whenNoProxies() {
        Set<String> set = Set.of("a", "b");
        Object result = HibernateProxyHandler.unproxyValueIfNeeded(set);

        assertThat(result).isSameAs(set);
    }

    // Tests for recreateCollection fallback paths

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldRecreateArrayList() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result)
                .isInstanceOf(ArrayList.class)
                .containsExactly("x", "y");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldRecreateHashSet() {
        Set<String> original = new HashSet<>(Set.of("a", "b"));
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result)
                .isInstanceOf(HashSet.class)
                .containsExactlyInAnyOrder("x", "y");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldRecreateLinkedList() {
        Queue<String> original = new LinkedList<>(List.of("a", "b"));
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result)
                .isInstanceOf(LinkedList.class)
                .containsExactly("x", "y");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldRecreateArrayDeque() {
        Deque<String> original = new ArrayDeque<>(List.of("a", "b"));
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result)
                .isInstanceOf(ArrayDeque.class)
                .containsExactly("x", "y");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldFallbackToArrayListForUnmodifiableList() {
        List<String> original = List.of("a", "b");
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result).containsExactly("x", "y");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateCollection_shouldFallbackToHashSetForUnmodifiableSet() {
        Set<String> original = Set.of("a", "b");
        List<String> unproxied = List.of("x", "y");

        Collection<String> result = (Collection<String>) HibernateProxyHandler.recreateCollection(original, unproxied);

        assertThat(result)
                .isInstanceOf(HashSet.class)
                .containsExactlyInAnyOrder("x", "y");
    }

    // Tests for recreateMap fallback paths

    @SuppressWarnings("unchecked")
    @Test
    void recreateMap_shouldRecreateHashMap() {
        Map<String, Integer> original = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> unproxied = Map.of("x", 2);

        Map<String, Integer> result = (Map<String, Integer>) HibernateProxyHandler.recreateMap(original, unproxied);

        assertThat(result)
                .isInstanceOf(HashMap.class)
                .containsEntry("x", 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateMap_shouldRecreateLinkedHashMap() {
        Map<String, Integer> original = new LinkedHashMap<>(Map.of("a", 1));
        Map<String, Integer> unproxied = Map.of("x", 2);

        Map<String, Integer> result = (Map<String, Integer>) HibernateProxyHandler.recreateMap(original, unproxied);

        assertThat(result)
                .isInstanceOf(LinkedHashMap.class)
                .containsEntry("x", 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateMap_shouldRecreateTreeMap() {
        Map<String, Integer> original = new TreeMap<>(Map.of("a", 1));
        Map<String, Integer> unproxied = Map.of("x", 2);

        Map<String, Integer> result = (Map<String, Integer>) HibernateProxyHandler.recreateMap(original, unproxied);

        assertThat(result)
                .isInstanceOf(TreeMap.class)
                .containsEntry("x", 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateMap_shouldRecreateConcurrentHashMap() {
        Map<String, Integer> original = new ConcurrentHashMap<>(Map.of("a", 1));
        Map<String, Integer> unproxied = Map.of("x", 2);

        Map<String, Integer> result = (Map<String, Integer>) HibernateProxyHandler.recreateMap(original, unproxied);

        assertThat(result)
                .isInstanceOf(ConcurrentHashMap.class)
                .containsEntry("x", 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void recreateMap_shouldFallbackToHashMapForUnmodifiableMap() {
        Map<String, Integer> original = Map.of("a", 1);
        Map<String, Integer> unproxied = Map.of("x", 2);

        Map<String, Integer> result = (Map<String, Integer>) HibernateProxyHandler.recreateMap(original, unproxied);

        assertThat(result)
                .isInstanceOf(HashMap.class)
                .containsEntry("x", 2);
    }
}
