package io.github.manoelcampos.dtogen.util;

import io.github.manoelcampos.dtogen.DTO;

import javax.lang.model.element.VariableElement;

import static io.github.manoelcampos.dtogen.AnnotationData.hasAnnotation;

/**
 * Utility methods to get information about a field.
 * @author Manoel Campos
 */
public final class FieldUtil {
    public static final String ID_FIELD_NOT_FOUND = "Cannot find id field in %s. Since the %s.%s is annotated with %s, it must be a class with an id field.";

    /** A private constructor to avoid class instantiation. */
    private FieldUtil(){/**/}

    /**
     * {@return the name of a field with the first letter in upper case}
     *
     * @param fieldName the name of a field
     */
    public static String getUpCaseFieldName(final String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    /**
     * {@return true if a given field is an instance field (not static), false otherwise}
     *
     * @param field the field to check
     */
    public static boolean isInstanceField(final VariableElement field) {
        return !field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
    }

    /**
     * Checks if the type of given field is a primitive type.
     * @param field the field to check
     * @return true if it's primitive, false otherwise
     */
    public static boolean isPrimitive(final VariableElement field) {
        return TypeUtil.isPrimitive(field.asType());
    }

    /**
     * {@return the name of a given field}
     * @param field the field to get its name
     */
    public static String getFieldName(final VariableElement field) {
        return field.getSimpleName().toString();
    }

    /**
     * {@return true if a field must not be excluded from the generated DTO record, false otherwise}
     * @param field the field to check
     */
    public static boolean isNotFieldExcluded(final VariableElement field) {
        return !isFieldExcluded(field);
    }

    /**
     * {@return true if a field must be excluded from the generated DTO record, false otherwise}
     * @param field the field to check
     */
    public static boolean isFieldExcluded(final VariableElement field) {
        return hasAnnotation(field, DTO.Exclude.class);
    }

    public static boolean isNotIdField(final VariableElement field) {
        return !"id".equals(getFieldName(field));
    }
}
