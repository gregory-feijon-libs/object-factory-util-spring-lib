package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests ensuring that exclusions configured for one (source, destination) pair
 * never leak into copies made for another pair through the reflection caches.
 */
@SpringBootTest(classes = {FactoryUtil.class, TestSerializerConfiguration.class})
class FieldResolverCacheIsolationTest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CacheSource {
        private String name;
        private String secret;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ObjectCopyExclusions({"secret"})
    public static class CacheRestrictedDest {
        private String name;
        private String secret;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CacheOpenSource {
        private String name;
        private String secret;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ObjectCopyExclusions({"secret"})
    public static class CacheOpenRestrictedDest {
        private String name;
        private String secret;
    }

    @Test
    void exclusionsOfOnePairShouldNotHideFieldsInAnotherPair() {
        CacheSource source = newSource(new CacheSource());

        CacheRestrictedDest restricted = ObjectFactoryUtil.createFromObject(source, CacheRestrictedDest.class);
        CacheSource sameTypeCopy = ObjectFactoryUtil.createFromObject(source);

        assertThat(restricted.getSecret()).isNull();
        assertThat(sameTypeCopy.getSecret()).isEqualTo("s3cr3t");
        assertThat(sameTypeCopy.getName()).isEqualTo("john");
    }

    @Test
    void cachedFullFieldSetShouldNotBypassExclusionsOfAnotherPair() {
        CacheOpenSource source = newSource(new CacheOpenSource());

        CacheOpenSource sameTypeCopy = ObjectFactoryUtil.createFromObject(source);
        CacheOpenRestrictedDest restricted = ObjectFactoryUtil.createFromObject(source, CacheOpenRestrictedDest.class);

        assertThat(sameTypeCopy.getSecret()).isEqualTo("s3cr3t");
        assertThat(restricted.getSecret()).isNull();
        assertThat(restricted.getName()).isEqualTo("john");
    }

    private static <T> T newSource(T target) {
        if (target instanceof CacheSource s) {
            s.setName("john");
            s.setSecret("s3cr3t");
        } else if (target instanceof CacheOpenSource s) {
            s.setName("john");
            s.setSecret("s3cr3t");
        }
        return target;
    }
}
