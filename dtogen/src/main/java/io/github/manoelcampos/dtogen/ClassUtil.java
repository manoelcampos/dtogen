package io.github.manoelcampos.dtogen;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.stream.Stream;

/**
 * Utility methods to get information about a class.
 * @author Manoel Campos
 */
public final class ClassUtil {
    private ClassUtil(){/**/}

    /**
     * {@return the name of a field with the first letter in upper case}
     * @param fieldName the name of a field
     */
    public static String getUpCaseFieldName(final String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    /**
     * {@return the package name of a class, or an empty string if the class has no package}
     */
    public static String getPackageName(final TypeElement classTypeElement) {
        final var qualifiedClassName = classTypeElement.getQualifiedName().toString();
        return getPackageName(qualifiedClassName);
    }

    /**
     * {@return the package name of a class, or an empty string if the class has no package}
     */
    public static String getPackageName(final String fullyQualifiedClassName){
        final int i = fullyQualifiedClassName.lastIndexOf('.');
        return i == -1 ? "" : fullyQualifiedClassName.substring(0, i);
    }

    /**
     * {@return the simple name of a class, without the package name}
     */
    public static String getSimpleClassName(final String fullyQualifiedClassName){
        final int i = fullyQualifiedClassName.lastIndexOf('.');
        return i == -1 ? fullyQualifiedClassName : fullyQualifiedClassName.substring(i+1);
    }

    /**
     * Checks if a given class has a superclass other than {@link java.lang.Object}.
     * @param classType the class to check
     * @return true if a given class has a superclass, false otherwise
     */
    public static boolean hasSuperClass(final TypeElement classType) {
        final TypeMirror superClassType = classType.getSuperclass();
        if(superClassType.getKind() == TypeKind.NONE)
            return false;

        final var qualifiedClassName = ((TypeElement) ((DeclaredType) superClassType).asElement()).getQualifiedName().toString();
        return !"java.lang.Object".equals(qualifiedClassName);
    }

    /**
     * {@return true if a given field is an instance field (not static), false otherwise}
     * @param field the field to check
     *
     */
    public static boolean isInstanceField(final VariableElement field) {
        return !field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
    }

    /**
     * Gets the fields of a given class, including the ones from its superclasses.
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     *
     * @param typeUtils        a reference to {@link DTOProcessor#typeUtils()}
     * @param classTypeElement the class to get the fields from
     * @return a stream of fields from the given class
     */
    public static Stream<VariableElement> getClassFields(final Types typeUtils, final TypeElement classTypeElement) {
        final var fieldStream =
                classTypeElement.getEnclosedElements().stream()
                                .filter(enclosedElement -> enclosedElement.getKind().isField())
                                .map(enclosedElement -> (VariableElement) enclosedElement)
                                .filter(ClassUtil::isInstanceField);

        final var superclassType = classTypeElement.getSuperclass();
        final var superclassElement = (TypeElement) typeUtils.asElement(superclassType);
        final var superClassFields = hasSuperClass(classTypeElement) ?
                                        getClassFields(typeUtils, superclassElement) :
                                        Stream.<VariableElement>empty();

        return Stream.concat(superClassFields, fieldStream);
    }

    public static boolean isPrimitive(final VariableElement field) {
        return isPrimitive(field.asType());
    }

    public static boolean isPrimitive(final TypeMirror fieldType) {
        return fieldType.getKind().isPrimitive();
    }

}
