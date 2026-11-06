package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.utils.serialization.HibernateProxyChecker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles Hibernate proxy unwrapping for objects, collections, and maps.
 * <p>
 * Delegates proxy detection to {@link HibernateProxyChecker} and only creates
 * new collection/map instances when proxies are actually found.
 * </p>
 *
 * @author gregory.feijon
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HibernateProxyHandler {

    /**
     * Removes Hibernate proxy wrappers from an object, if present.
     * Recursively unwraps proxies from objects, collections, and maps.
     *
     * @param value the object to unproxy
     * @return the unproxied object, or the original if no proxy was found
     */
    @SuppressWarnings("unchecked")
    public static Object unproxyValueIfNeeded(Object value) {
        if (!HibernateProxyChecker.isHibernateAvailable()) {
            return value; // no proxies can exist: skip scanning collections/maps entirely
        }
        if (HibernateProxyChecker.isHibernateProxy(value)) {
            return HibernateProxyChecker.unproxy(value);
        }
        return switch (value) {
            case Collection<?> collection -> unproxyCollection((Collection<Object>) collection);
            case Map<?, ?> map -> unproxyMap((Map<Object, Object>) map);
            case null, default -> value;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> unproxyCollection(Collection<T> collection) {
        if (!containsProxy(collection)) {
            return collection;
        }

        List<T> unproxied = collection.stream()
                .map(item -> (T) unproxyValueIfNeeded(item))
                .toList();

        return recreateCollection(collection, unproxied);
    }

    @SuppressWarnings("unchecked")
    private static <T, S> Map<T, S> unproxyMap(Map<T, S> map) {
        if (!containsProxy(map)) {
            return map;
        }

        Map<T, S> unproxied = map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> (T) unproxyValueIfNeeded(e.getKey()),
                        e -> (S) unproxyValueIfNeeded(e.getValue())
                ));

        return recreateMap(map, unproxied);
    }

    private static <T> boolean containsProxy(Collection<T> collection) {
        return collection.stream()
                .anyMatch(HibernateProxyChecker::isHibernateProxy);
    }

    private static <T, S> boolean containsProxy(Map<T, S> map) {
        return map.entrySet().stream()
                .anyMatch(e -> HibernateProxyChecker.isHibernateProxy(e.getKey())
                        || HibernateProxyChecker.isHibernateProxy(e.getValue()));
    }

    static <T> Collection<T> recreateCollection(Collection<T> original, List<T> unproxied) {
        Collection<T> result = recreateSameType(original, copy -> copy.addAll(unproxied));
        if (result != null) {
            return result;
        }
        return switch (original) {
            case Set<T> ignored -> new HashSet<>(unproxied);
            case Deque<T> ignored -> new ArrayDeque<>(unproxied);
            case Queue<T> ignored -> new LinkedList<>(unproxied);
            default -> new ArrayList<>(unproxied);
        };
    }

    static <T, S> Map<T, S> recreateMap(Map<T, S> original, Map<T, S> unproxied) {
        Map<T, S> result = recreateSameType(original, copy -> copy.putAll(unproxied));
        if (result != null) {
            return result;
        }
        return switch (original) {
            case LinkedHashMap<T, S> ignored -> new LinkedHashMap<>(unproxied);
            case TreeMap<T, S> ignored -> new TreeMap<>(unproxied);
            case ConcurrentHashMap<T, S> ignored -> new ConcurrentHashMap<>(unproxied);
            default -> new HashMap<>(unproxied);
        };
    }

    /**
     * Tries to instantiate the exact runtime type of {@code original} through its no-arg constructor
     * and fill it with {@code filler}.
     *
     * @return the filled instance, or {@code null} when the type cannot be instantiated or filled
     */
    @SuppressWarnings("unchecked")
    private static <C> C recreateSameType(C original, Consumer<C> filler) {
        try {
            C copy = (C) original.getClass().getDeclaredConstructor().newInstance();
            filler.accept(copy);
            return copy;
        } catch (Exception e) {
            log.trace("Could not instantiate exact type {}. Using type-based fallback.",
                    original.getClass().getSimpleName());
            return null;
        }
    }
}
