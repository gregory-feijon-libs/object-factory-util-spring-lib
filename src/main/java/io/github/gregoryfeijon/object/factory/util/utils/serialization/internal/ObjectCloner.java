package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil;
import io.github.gregoryfeijon.object.factory.util.utils.serialization.ObjectFactoryUtil;
import io.github.gregoryfeijon.serializer.provider.util.serialization.SerializationUtil;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerAdapter;
import io.github.gregoryfeijon.serializer.provider.util.serialization.adapter.SerializerProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.SerializationUtils;

/**
 * Handles object cloning via binary and JSON serialization strategies.
 * <p>
 * Selects the appropriate serialization strategy based on the relationship
 * between source and destination types:
 * </p>
 * <ul>
 *   <li>Simple/wrapper types: Java binary serialization</li>
 *   <li>Same type (deep clone): JSON serialization</li>
 *   <li>Different types (conversion): Recursive {@link ObjectFactoryUtil#createFromObject}</li>
 * </ul>
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObjectCloner {

    private static final SerializerAdapter SERIALIZER = SerializerProvider.getAdapter();

    /**
     * Creates a serialized clone of a wrapper type value.
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value, or null if the source value is null
     */
    public static Object serializingClone(Object sourceValue, Class<?> clazz) {
        return sourceValue != null ? serializingCloneObjects(sourceValue, clazz) : null;
    }

    /**
     * Creates a deep copy of an object via serialization.
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value
     */
    public static Object serializingCloneObjects(Object sourceValue, Class<?> clazz) {
        if (sourceValue == null) {
            return null;
        }
        Class<?> sourceClass = sourceValue.getClass();

        if (ReflectionTypeUtil.isSimpleType(clazz)) {
            return cloneToBinaryFormat(sourceValue);
        }

        if (ReflectionTypeUtil.isWrapperType(sourceClass)) {
            return cloneToBinaryFormat(sourceValue);
        }

        if (sourceClass == clazz) {
            return cloneToJsonFormat(sourceValue, clazz);
        }

        return ObjectFactoryUtil.createFromObject(sourceValue, clazz);
    }

    /**
     * Clones an object using Java binary serialization.
     *
     * @param sourceValue the value to clone
     * @return a deep copy of the source value
     */
    static Object cloneToBinaryFormat(Object sourceValue) {
        byte[] byteClone = SerializationUtils.serialize(sourceValue);
        return SerializationUtil.deserialize(byteClone);
    }

    /**
     * Clones an object using JSON serialization.
     *
     * @param sourceValue the value to clone
     * @param clazz       the target class type
     * @return a deep copy of the source value
     */
    static Object cloneToJsonFormat(Object sourceValue, Class<?> clazz) {
        return SERIALIZER.deserialize(SERIALIZER.serialize(sourceValue), clazz);
    }

    /**
     * Returns the shared serializer adapter instance.
     *
     * @return the serializer adapter
     */
    public static SerializerAdapter getSerializer() {
        return SERIALIZER;
    }
}
