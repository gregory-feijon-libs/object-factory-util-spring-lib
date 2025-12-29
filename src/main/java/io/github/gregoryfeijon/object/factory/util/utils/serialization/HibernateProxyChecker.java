package io.github.gregoryfeijon.object.factory.util.utils.serialization;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * Utility class to check and handle Hibernate proxies without requiring Hibernate as a mandatory dependency.
 * <p>
 * This class uses reflection to avoid ClassNotFoundException when Hibernate is not in the classpath.
 * All Hibernate-specific operations are isolated here to allow the library to work in projects
 * with or without Hibernate.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HibernateProxyChecker {

    private static final Class<?> HIBERNATE_PROXY_CLASS;
    private static final Class<?> LAZY_INITIALIZER_CLASS;
    private static final Method GET_LAZY_INITIALIZER_METHOD;
    private static final Method IS_UNINITIALIZED_METHOD;
    private static final Method GET_IMPLEMENTATION_METHOD;
    private static final Method GET_PERSISTENT_CLASS_METHOD;

    static {
        Class<?> proxyClass = null;
        Class<?> lazyInitClass = null;
        Method getLazyInitMethod = null;
        Method isUninitMethod = null;
        Method getImplMethod = null;
        Method getPersistentClassMethod = null;

        try {
            proxyClass = Class.forName("org.hibernate.proxy.HibernateProxy");
            lazyInitClass = Class.forName("org.hibernate.proxy.LazyInitializer");

            getLazyInitMethod = proxyClass.getMethod("getHibernateLazyInitializer");
            isUninitMethod = lazyInitClass.getMethod("isUninitialized");
            getImplMethod = lazyInitClass.getMethod("getImplementation");
            getPersistentClassMethod = lazyInitClass.getMethod("getPersistentClass");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Hibernate não está disponível - isso é OK
        }

        HIBERNATE_PROXY_CLASS = proxyClass;
        LAZY_INITIALIZER_CLASS = lazyInitClass;
        GET_LAZY_INITIALIZER_METHOD = getLazyInitMethod;
        IS_UNINITIALIZED_METHOD = isUninitMethod;
        GET_IMPLEMENTATION_METHOD = getImplMethod;
        GET_PERSISTENT_CLASS_METHOD = getPersistentClassMethod;
    }

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
        return HIBERNATE_PROXY_CLASS != null
                && LAZY_INITIALIZER_CLASS != null
                && GET_LAZY_INITIALIZER_METHOD != null
                && IS_UNINITIALIZED_METHOD != null
                && GET_IMPLEMENTATION_METHOD != null
                && GET_PERSISTENT_CLASS_METHOD != null;
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
        return isHibernateAvailable()
                && HIBERNATE_PROXY_CLASS.isInstance(value);
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
            Object lazyInitializer = GET_LAZY_INITIALIZER_METHOD.invoke(proxy);
            boolean isUninitialized = (boolean) IS_UNINITIALIZED_METHOD.invoke(lazyInitializer);

            if (isUninitialized) {
                Class<?> persistentClass = (Class<?>) GET_PERSISTENT_CLASS_METHOD.invoke(lazyInitializer);
                return org.springframework.beans.BeanUtils.instantiateClass(persistentClass);
            }

            return GET_IMPLEMENTATION_METHOD.invoke(lazyInitializer);
        } catch (Exception e) {
            return proxy;
        }
    }
}