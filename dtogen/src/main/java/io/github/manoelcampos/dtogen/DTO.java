package io.github.manoelcampos.dtogen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates a record representing a Data Type Object (DTO) for the annotated class.
 * @author Manoel Campos
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DTO {
    /**
     * Indicates that the annotated field must be excluded from the generated DTO.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Exclude {
    }

    /**
     * Indicates that the annotated field must be mapped to the DTO as an ID.
     * This way, the annotation must be used in a field whose type is a class
     * containing an id.
     * If the annotated field is declared as, for instance, {@code Country country},
     * the generated DTO will have a field {@code long countryId}.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface MapToId {
    }
}
