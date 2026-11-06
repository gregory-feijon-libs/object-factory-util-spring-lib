package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.factory.FactoryUtil;
import io.github.gregoryfeijon.object.factory.util.config.TestSerializerConfiguration;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclude;
import io.github.gregoryfeijon.object.factory.util.domain.annotation.ObjectCopyExclusions;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hierarchy-aware field matching: a field hidden (shadowed) by a subclass is an independent
 * slot of state and must be copied faithfully.
 */
@SpringBootTest(classes = {FactoryUtil.class, TestSerializerConfiguration.class})
class FieldHidingMappingTest {

    public static class Parent {
        protected String code;

        public String parentCode() {
            return code;
        }

        void setParentCode(String value) {
            code = value;
        }
    }

    public static class Child extends Parent {
        private String code;

        public String childCode() {
            return code;
        }

        Child withCodes(String parentValue, String childValue) {
            setParentCode(parentValue);
            code = childValue;
            return this;
        }
    }

    public static class OtherParent {
        protected String code;

        public String parentCode() {
            return code;
        }
    }

    public static class OtherChild extends OtherParent {
        private String code;

        public String childCode() {
            return code;
        }
    }

    public static class Flat {
        private String code;

        public String code() {
            return code;
        }
    }

    @ObjectCopyExclusions({"code"})
    public static class ExcludingDest extends Parent {
        private String code;

        public String childCode() {
            return code;
        }
    }

    public static class DestWithExcludedChildSlot extends Parent {
        @ObjectCopyExclude
        private String code;

        public String childCode() {
            return code;
        }
    }

    @Test
    void sameClassCopy_shouldPreserveBothHiddenSlots() {
        Child copy = ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"));

        assertThat(copy.childCode()).isEqualTo("C");
        assertThat(copy.parentCode()).isEqualTo("P");
    }

    @Test
    void unrelatedHierarchies_shouldPairSlotsLevelByLevel() {
        OtherChild copy = ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"), OtherChild.class);

        assertThat(copy.childCode()).isEqualTo("C");
        assertThat(copy.parentCode()).isEqualTo("P");
    }

    @Test
    void hiddenSourceToFlatDest_shouldUseMostDerivedValue() {
        Flat copy = ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"), Flat.class);

        assertThat(copy.code()).isEqualTo("C");
    }

    @Test
    void childToParent_shouldFillParentSlotWithParentState() {
        Parent copy = ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"), Parent.class);

        assertThat(copy.parentCode()).isEqualTo("P");
    }

    @Test
    void parentToChild_shouldFillOnlyParentSlot() {
        Parent source = new Parent();
        source.setParentCode("P");

        Child copy = ObjectFactoryUtil.createFromObject(source, Child.class);

        assertThat(copy.parentCode()).isEqualTo("P");
        assertThat(copy.childCode()).isNull();
    }

    @Test
    void classLevelExclusion_shouldExcludeEveryFieldWithThatKey() {
        ExcludingDest copy = ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"), ExcludingDest.class);

        assertThat(copy.childCode()).isNull();
        assertThat(copy.parentCode()).isNull();
    }

    @Test
    void fieldLevelDestExclusion_shouldExcludeOnlyThatSlot() {
        DestWithExcludedChildSlot copy =
                ObjectFactoryUtil.createFromObject(new Child().withCodes("P", "C"), DestWithExcludedChildSlot.class);

        assertThat(copy.childCode()).isNull();
        assertThat(copy.parentCode()).isEqualTo("P");
    }
}
