package io.github.gregoryfeijon.object.factory.util.utils.serialization;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Method;

/**
 * Utility class to check and handle Hibernate proxies without requiring Hibernate as a mandatory dependency.
 * <p>
 * This class uses reflection to avoid ClassNotFoundException when Hibernate is not in the classpath.
 * All Hibernate-specific operations are isolated here to allow the library to work in projects
 * with or without Hibernate. The reflective handles are resolved once, at class initialization.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HibernateProxyChecker {

    /**
     * Reflective handles to the Hibernate proxy API, or {@code null} when Hibernate is absent.
     */
    private static final HibernateApi HIBERNATE = HibernateApi.load();

    /**
     * Checks if Hibernate is available in the classpath.
     * <p>
     * This method verifies that all required Hibernate classes and methods are available
     * to ensure safe operation of proxy unwrapping functionality.
     * </p>
     *
     * @return {@code true} if Hibernate is fully available, {@code false} otherwise
     */
    public static boolean isHibernateAvailable() {
        return HIBERNATE != null;
    }

    /**
     * Checks if an object is a Hibernate proxy.
     * <p>
     * Returns {@code false} if Hibernate is not available in the classpath.
     * </p>
     *
     * @param value the object to check
     * @return {@code true} if the object is a Hibernate proxy, {@code false} otherwise
     */
    public static boolean isHibernateProxy(Object value) {
        return HIBERNATE != null && HIBERNATE.proxyClass().isInstance(value);
    }

    /**
     * Unwraps a Hibernate proxy to its underlying implementation.
     * <p>
     * If the proxy is uninitialized, returns a new instance of the persistent class
     * to avoid triggering lazy loading. Otherwise, returns the initialized implementation.
     * </p>
     * <p>
     * If Hibernate is not available or the object is not a proxy, returns the original object.
     * </p>
     *
     * @param proxy the Hibernate proxy to unwrap
     * @return the underlying object, a new instance if uninitialized, or the original object
     */
    public static Object unproxy(Object proxy) {
        if (!isHibernateProxy(proxy)) {
            return proxy;
        }
        try {
            Object lazyInitializer = HIBERNATE.getLazyInitializer().invoke(proxy);
            if ((boolean) HIBERNATE.isUninitialized().invoke(lazyInitializer)) {
                Class<?> persistentClass = (Class<?>) HIBERNATE.getPersistentClass().invoke(lazyInitializer);
                return BeanUtils.instantiateClass(persistentClass);
            }
            return HIBERNATE.getImplementation().invoke(lazyInitializer);
        } catch (Exception e) {
            return proxy;
        }
    }

    private record HibernateApi(Class<?> proxyClass,
                                Method getLazyInitializer,
                                Method isUninitialized,
                                Method getImplementation,
                                Method getPersistentClass) {

        @SuppressWarnings("java:S1166")
        static HibernateApi load() {
            try {
                Class<?> proxyClass = Class.forName("org.hibernate.proxy.HibernateProxy");
                Class<?> lazyInitClass = Class.forName("org.hibernate.proxy.LazyInitializer");
                return new HibernateApi(proxyClass,
                        proxyClass.getMethod("getHibernateLazyInitializer"),
                        lazyInitClass.getMethod("isUninitialized"),
                        lazyInitClass.getMethod("getImplementation"),
                        lazyInitClass.getMethod("getPersistentClass"));
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
                return null; // Hibernate is not on the classpath - proxy handling is disabled
            }
        }
    }
}
