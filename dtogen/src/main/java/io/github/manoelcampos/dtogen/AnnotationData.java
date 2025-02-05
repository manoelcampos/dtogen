package io.github.manoelcampos.dtogen;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;

/**
 * Stores data about an annotation.
 * @param name the fully qualified name of the annotation, without the @
 * @param values values between the parenthesis of the annotation, if any.
 * @author Manoel Campos
 */
public record AnnotationData(String name, String values) {
    /**
     * Gets the string representation of the annotations of a field.
     *
     * @param sourceFieldAnnotations
     * @return
     */
    public static String getFieldAnnotationsStr(final List<AnnotationData> sourceFieldAnnotations) {
        return sourceFieldAnnotations
                .stream()
                .map(AnnotationData::toString)
                .collect(joining(" "));
    }

    /**
     * Gets the annotations of a field.
     * @param field the field to get its annotations
     * @param annotationPredicate a Predicate to filter the annotations you want to keep for a field
     * @return
     */
    public static List<AnnotationData> getFieldAnnotations(final VariableElement field, final Predicate<AnnotationData> annotationPredicate) {
        return field
                .getAnnotationMirrors()
                .stream()
                .map(AnnotationData::get)
                .filter(annotationPredicate)
                .toList();
    }


    private static AnnotationData get(final AnnotationMirror mirror) {
        final var annotationType = mirror.getAnnotationType().toString();
        final var annotationValues = ElementFilter.methodsIn(mirror.getElementValues().keySet())
                                                  .stream()
                                                  .map(element -> attributeToStr(element, mirror))
                                                  .collect(joining(", "));

        return new AnnotationData(annotationType, annotationValues);
    }

    /**
     * Gets a String representation of a given annotation attribute.
     *
     * @param annotationAttributeElement annotation attribute to get a String representation
     * @param mirror the annotation mirror that enables getting annotation metadata
     * @return
     */
    private static String attributeToStr(final ExecutableElement annotationAttributeElement, final AnnotationMirror mirror) {
        final Object value = mirror.getElementValues().get(annotationAttributeElement).getValue();
        return annotationAttributeElement.getSimpleName() + "=" + formatAttrValue(value);
    }

    /**
     * Formats a value for some attribute in the annotation, adding quotes if that value is a String or char.
     * @param value the value to format
     * @return the formatted value
     */
    static String formatAttrValue(final Object value) {
        if (value instanceof String)
            return String.format("\"%s\"", value);

        if (value instanceof Character)
            return String.format("'%c'", value);

        return value.toString();
    }

    /**
     * Checks if an annotation mirror represents a given annotation class,
     * that is, if a given annotation is the one we are looking for.
     *
     * @param annotationClass the class of the annotation we are looking for
     * @param mirror          the annotation mirror that enables getting annotation metadata
     * @return
     */
    public static boolean isEqualTo(final Class<?> annotationClass, final AnnotationMirror mirror) {
        return mirror.getAnnotationType().toString().endsWith(getName(annotationClass));
    }

    /**
     * Gets the fully qualified name of an annotation class, replacing the dollar sign ($) by a dot (.) for annotations
     * defined inside other ones (such as {@link DTO.MapToId}).
     * @param annotationClassClass the annotation class to get its name
     * @return the fully qualified annotation name
     */
    public static String getName(final Class<?> annotationClassClass) {
        return annotationClassClass.getName().replaceAll("\\$", ".");
    }

    /**
     * Checks if a class field has a given annotation.
     * @param sourceField the field to look for an annotation
     * @param annotationClass the annotation we are looking for in the field
     * @return
     */
    public static boolean contains(final VariableElement sourceField, final Class<? extends Annotation> annotationClass) {
        return sourceField.getAnnotationMirrors().stream().anyMatch(mirror -> isEqualTo(annotationClass, mirror));
    }

    /**
     * {@return the simple name of the annotation, without the package name}
     */
    public String getSimpleName(){
        return ClassUtil.getSimpleClassName(name);
    }

    @Override
    public String toString() {
        return String.format("@%s(%s)", getSimpleName(), values);
    }
}
