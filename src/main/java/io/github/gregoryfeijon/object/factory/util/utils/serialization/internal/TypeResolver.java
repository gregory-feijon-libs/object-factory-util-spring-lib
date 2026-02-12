package io.github.gregoryfeijon.object.factory.util.utils.serialization.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isPrimitiveOrEnum;
import static io.github.gregoryfeijon.object.factory.commons.utils.ReflectionTypeUtil.isWrapperType;

/**
 * Resolves and validates generic type information for collections, maps,
 * and nested parameterized types during object copy operations.
 *
 * @author gregory.feijon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeResolver {

    /**
     * Verifies that the generic type parameters of a parameterized type are instantiable
     * and returns them as an array of Class objects.
     *
     * @param genericType the parameterized type to verify
     * @return an array of {@link Class} objects representing the type parameters
     * @throws ClassNotFoundException     if a type parameter class cannot be found
     * @throws BeanInstantiationException if a type parameter cannot be instantiated
     */
    public static Class<?>[] verifyAndExtractTypes(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        ParameterizedType paramType = (ParameterizedType) genericType;
        Type[] typeArgs = paramType.getActualTypeArguments();
        Class<?>[] classes = new Class<?>[typeArgs.length];

        for (int i = 0; i < typeArgs.length; i++) {
            Class<?> clazz = extractElementClass(typeArgs[i]);
            if (!isPrimitiveOrEnum(clazz) && !isWrapperType(clazz)) {
                BeanUtils.instantiateClass(clazz);
            }
            classes[i] = clazz;
        }

        return classes;
    }

    /**
     * Verifies that the generic type parameters of a collection are instantiable.
     *
     * @param genericType the generic type to verify
     * @throws ClassNotFoundException     if a type cannot be found
     * @throws BeanInstantiationException if a type cannot be instantiated
     */
    public static void verifyType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        verifyAndExtractTypes(genericType);
    }

    /**
     * Extracts the innermost element class from a type, handling nested generic types.
     * <p>
     * For example:
     * </p>
     * <ul>
     *   <li>{@code String} → {@code String.class}</li>
     *   <li>{@code List<String>} → {@code String.class}</li>
     *   <li>{@code List<List<String>>} → {@code String.class}</li>
     * </ul>
     *
     * @param type the type to extract from
     * @return the innermost element class
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class<?> extractElementClass(Type type) throws ClassNotFoundException {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                return extractElementClass(typeArgs[0]);
            }
            return ClassUtils.getClass(paramType.getRawType().getTypeName());
        }
        return ClassUtils.getClass(type.getTypeName());
    }

    /**
     * Extracts the element type from a collection generic type.
     * For {@code List<T>}, returns {@code T.class}.
     *
     * @param genericType the collection's generic type
     * @return the element class
     * @throws ClassNotFoundException     if type resolution fails
     * @throws BeanInstantiationException if type instantiation fails
     */
    public static Class<?> extractCollectionElementType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        return verifyAndExtractTypes(genericType)[0];
    }

    /**
     * Extracts the value type from a map generic type.
     * For {@code Map<K, V>}, returns {@code V.class}.
     *
     * @param genericType the map's generic type
     * @return the value class
     * @throws ClassNotFoundException     if type resolution fails
     * @throws BeanInstantiationException if type instantiation fails
     */
    public static Class<?> extractMapValueType(Type genericType)
            throws ClassNotFoundException, BeanInstantiationException {
        return verifyAndExtractTypes(genericType)[1];
    }

    /**
     * Gets the innermost element type of a nested structure.
     * <p>
     * For structures like {@code List<List<List<PrimitiveFoo>>>}, returns {@code PrimitiveFoo.class}.
     * </p>
     *
     * @param element the element to inspect
     * @return the class of the innermost element
     */
    public static Class<?> getInnermostElementType(Object element) {
        return switch (element) {
            case null -> Object.class;
            case Collection<?> collection -> collection.isEmpty()
                    ? Object.class
                    : getInnermostElementType(collection.iterator().next());
            case Map<?, ?> map -> map.isEmpty()
                    ? Object.class
                    : getInnermostElementType(map.values().iterator().next());
            default -> element.getClass();
        };
    }

    /**
     * Extracts the nested generic type at the specified index from a parameterized type.
     * <p>
     * For {@code List<T>}, index 0 returns T.
     * For {@code Map<K, V>}, index 0 returns K and index 1 returns V.
     * </p>
     *
     * @param genericType the original generic type
     * @param index       the index of the type argument
     * @return the generic type at the specified index
     */
    public static Type getNestedGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > index) {
                return typeArgs[index];
            }
        }
        return Object.class;
    }
}
