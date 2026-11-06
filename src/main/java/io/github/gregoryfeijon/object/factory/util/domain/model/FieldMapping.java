package io.github.gregoryfeijon.object.factory.util.domain.model;

import java.lang.reflect.Field;

/**
 * Immutable pairing between a source field and the destination field that receives its value.
 * <p>
 * Instances are resolved once per (source class, destination class) pair and reused for every
 * subsequent copy between those types.
 * </p>
 *
 * @param sourceField the field read from the source object
 * @param destField   the field written on the destination object
 * @author gregory.feijon
 */
public record FieldMapping(Field sourceField, Field destField) {
}
