package io.github.manoelcampos.dtogen;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Manoel Campos
 */
public final class ClassUtil {
    private ClassUtil(){/**/}

    public static String getUpCaseFieldName(final String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static String getPackageName(final TypeElement classTypeElement) {
        final var qualifiedClassName = classTypeElement.getQualifiedName().toString();
        return qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf("."));
    }

    public static boolean hasSuperClass(final TypeMirror superclassType) {
        final var qualifiedClasname = ((TypeElement) ((DeclaredType) superclassType).asElement()).getQualifiedName().toString();
        return superclassType.getKind() != TypeKind.NONE && !"java.lang.Object".equals(qualifiedClasname);
    }

    public static boolean isInstanceField(final VariableElement field) {
        return !field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
    }

    /**
     * Gets the fields of a given class, including the ones from its superclasses.
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     * @param typeUtils
     * @param classTypeElement
     * @param fieldPredicate a predicate to indicate how to select class fields
     * @return
     */
    public static Stream<VariableElement> getClassFields(final Types typeUtils, final TypeElement classTypeElement, final Predicate<VariableElement> fieldPredicate) {
        final var fieldStream =
                classTypeElement.getEnclosedElements().stream()
                                .filter(enclosedElement -> enclosedElement.getKind().isField())
                                .map(enclosedElement -> (VariableElement) enclosedElement)
                                .filter(fieldPredicate)
                                .filter(ClassUtil::isInstanceField);

        final var superclassType = classTypeElement.getSuperclass();
        final var superclassElement = (TypeElement) typeUtils.asElement(superclassType);
        final var superClassFields = hasSuperClass(superclassType) ?
                                        getClassFields(typeUtils, superclassElement, fieldPredicate) :
                                        Stream.<VariableElement>empty();

        return Stream.concat(superClassFields, fieldStream);
    }
}
