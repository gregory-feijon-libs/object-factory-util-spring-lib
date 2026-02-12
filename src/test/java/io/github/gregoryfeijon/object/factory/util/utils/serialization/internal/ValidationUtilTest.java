package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.util.exception.ApiException;
import io.github.gregoryfeijon.object.factory.util.exception.ErrorMessages;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilTest {

    @Test
    void verifySourceAndDestObjects_shouldPassWhenBothNonNull() {
        assertThatCode(() -> ValidationUtil.verifySourceAndDestObjects("source", "dest"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifySourceAndDestObjects_shouldThrowWhenSourceIsNull() {
        assertThatThrownBy(() -> ValidationUtil.verifySourceAndDestObjects(null, "dest"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.SOURCE_OBJECT_NULL);
    }

    @Test
    void verifySourceAndDestObjects_shouldThrowWhenDestIsNull() {
        assertThatThrownBy(() -> ValidationUtil.verifySourceAndDestObjects("source", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.DESTINATION_OBJECT_NULL);
    }

    @Test
    void verifySourceAndDestObjects_shouldThrowWhenBothNull() {
        assertThatThrownBy(() -> ValidationUtil.verifySourceAndDestObjects(null, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.SOURCE_OBJECT_NULL);
    }

    @Test
    void verifySourceObject_shouldPassWhenNonNull() {
        assertThatCode(() -> ValidationUtil.verifySourceObject("source"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifySourceObject_shouldThrowWhenNull() {
        assertThatThrownBy(() -> ValidationUtil.verifySourceObject(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.SOURCE_OBJECT_NULL);
    }

    @Test
    void verifyCollection_shouldPassWhenNonEmpty() {
        assertThatCode(() -> ValidationUtil.verifyCollection(List.of("a")))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyCollection_shouldThrowWhenEmpty() {
        assertThatThrownBy(() -> ValidationUtil.verifyCollection(Collections.emptyList()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.COLLECTION_EMPTY);
    }

    @Test
    void verifyCollection_shouldThrowWhenNull() {
        assertThatThrownBy(() -> ValidationUtil.verifyCollection(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.COLLECTION_EMPTY);
    }

    @Test
    void verifyCollectionAndSupplier_shouldPassWhenBothValid() {
        Supplier<List<String>> supplier = ArrayList::new;
        assertThatCode(() -> ValidationUtil.verifyCollectionAndSupplier(List.of("a"), supplier))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyCollectionAndSupplier_shouldThrowWhenCollectionEmpty() {
        Supplier<List<String>> supplier = ArrayList::new;
        assertThatThrownBy(() -> ValidationUtil.verifyCollectionAndSupplier(Collections.emptyList(), supplier))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.COLLECTION_EMPTY);
    }

    @Test
    void verifyCollectionAndSupplier_shouldThrowWhenSupplierIsNull() {
        assertThatThrownBy(() -> ValidationUtil.verifyCollectionAndSupplier(List.of("a"), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessages.SUPPLIER_NULL);
    }
}
