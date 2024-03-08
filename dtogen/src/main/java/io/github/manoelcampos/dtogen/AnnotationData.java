package io.github.manoelcampos.dtogen;

/**
 * Stores data about an annotation.
 * @param name name of the annotation, without the @
 * @param values values between the parenthesis of the annotation, if any.
 * @author Manoel Campos
 */
public record AnnotationData(String name, String values) {
    @Override
    public String toString() {
        return String.format("@%s(%s)", name, values);
    }
}
