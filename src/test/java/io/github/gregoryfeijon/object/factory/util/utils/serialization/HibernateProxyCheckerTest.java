package io.github.gregoryfeijon.object.factory.util.utils.serialization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateProxyCheckerTest {

    @Test
    void isHibernateAvailable_shouldReturnTrue_whenHibernateInClasspath() {
        // Hibernate is in the classpath (provided scope in pom.xml)
        assertThat(HibernateProxyChecker.isHibernateAvailable()).isTrue();
    }

    @Test
    void isHibernateProxy_shouldReturnFalse_forRegularObject() {
        assertThat(HibernateProxyChecker.isHibernateProxy("regular string")).isFalse();
    }

    @Test
    void isHibernateProxy_shouldReturnFalse_forNull() {
        assertThat(HibernateProxyChecker.isHibernateProxy(null)).isFalse();
    }

    @Test
    void isHibernateProxy_shouldReturnFalse_forPrimitiveWrapper() {
        assertThat(HibernateProxyChecker.isHibernateProxy(42)).isFalse();
    }

    @Test
    void unproxy_shouldReturnSameObject_whenNotProxy() {
        String original = "not a proxy";
        Object result = HibernateProxyChecker.unproxy(original);

        assertThat(result).isSameAs(original);
    }

    @Test
    void unproxy_shouldReturnNull_whenNullPassed() {
        Object result = HibernateProxyChecker.unproxy(null);

        assertThat(result).isNull();
    }

    @Test
    void unproxy_shouldReturnSameCollection_whenNotProxy() {
        var list = java.util.List.of("a", "b");
        Object result = HibernateProxyChecker.unproxy(list);

        assertThat(result).isSameAs(list);
    }
}
