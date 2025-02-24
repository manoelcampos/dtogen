package io.github.manoelcampos.dtogen.util;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.DTOProcessor;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * Utility methods to get information about types such as a class/record/interface.
 * @author Manoel Campos
 */
public final class TypeUtil {
    private final DTOProcessor processor;

    public TypeUtil(final DTOProcessor processor) {
        this.processor = processor;
    }

    /**
     * {@return a TypeElement that represents a field type, or null if the field is primitive}
     * @param fieldElement the element representing the field.
     */
    public @Nullable TypeElement getClassTypeElement(final VariableElement fieldElement) {
        return getTypeMirrorAsElement(fieldElement.asType());
    }

    public TypeElement getTypeMirrorAsElement(final TypeMirror typeMirror) {
        return (TypeElement) processor.types().asElement(typeMirror);
    }

    /**
     * Gets the qualified name of a type (including possible generic type arguments)
     * based on a {@link VariableElement} representing a variable/field declared as that type.
     * @param fieldElement variable/field to get its type
     * @return the type name
     */
    public String getTypeName(final VariableElement fieldElement) {
        return getTypeName(fieldElement, true, true);
    }

    /**
     * Gets the name of a type based on a {@link VariableElement} representing a variable/field declared as that type.
     * @param fieldElement variable/field to get its type
     * @param qualified if the type name must include the full-qualified package name or just the type name
     * @param includeTypeArgs indicates if the returned type name must include possible generic type arguments
     * @return the type name
     */
    public String getTypeName(final VariableElement fieldElement, final boolean qualified, final boolean includeTypeArgs) {
        final var typeMirror = fieldElement.asType();
        if (FieldUtil.isPrimitive(typeMirror)) {
            return typeMirror.getKind().toString().toLowerCase();
        }

        final var declaredType = getAsDeclaredType(typeMirror);
        if (declaredType == null) {
            return typeMirror.toString();
        }

        final var element = (TypeElement) declaredType.asElement();
        // Check if the type has generic parameters
        final String typeArguments = includeTypeArgs ? genericTypeArguments(declaredType) : "";
        final var name = qualified ? element.getQualifiedName() : element.getSimpleName();
        return name + typeArguments;
    }

    /**
     * Gets the generic type arguments of a given type.
     * If the type is {@code List<Customer>}, List is the declared type and Customer is the generic argument.
     *
     * @param declaredType the type to get its generic arguments
     * @return a String representing all the generic type arguments in format {@code <Type1, TypeN>};
     *         or an empty string if there are no generic type arguments.
     */
    private String genericTypeArguments(final DeclaredType declaredType) {
        final var typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return "";
        }

        // Recursively gets the generic type arguments for each generic type argument.
        final var genericTypeArgs =
                typeArguments
                        .stream()
                        .map(this::getGenericTypeArgTypes)
                        .collect(joining(", "));

        return "<" + genericTypeArgs + ">";
    }

    /**
     * Gets the type arguments of a generic type arg.
     * If the original type is something such as Type1<Type2<Type3>, Type4>,
     * it gets a string representation of Type1 with its 2 types: Type2<Type3> and Type4.
     * Since Type2 has its own generic type argument as well,
     * the method returns a String Type1<Type2<Type3>, Type4>
     * instead of just Type1<Type2, Type4>.
     * @param genericTypeArg the generic type argument to get its own type arguments (if any)
     * @return a string representation of the generic type argument with its own type arguments (if any)
     */
    private String getGenericTypeArgTypes(final TypeMirror genericTypeArg) {
        final var declaredGenericTypeArg = (DeclaredType) genericTypeArg;
        // Gets the generic types of the generic type arg
        final var genericSubTypes = genericTypeArguments(declaredGenericTypeArg);
        return "%s%s".formatted(genericTypeArgument(genericTypeArg), genericSubTypes);
    }

    /**
     * @see #genericTypeArguments(DeclaredType)
     */
    private String genericTypeArgument(final TypeMirror genericType) {
        final var genericTypeElement = getTypeMirrorAsElement(genericType);
        final var qName = genericTypeElement.getQualifiedName().toString();
        final var finalName = gen.isFieldClassPkgSameAsRecordPkg(qName) ? getSimpleClassName(qName) : qName;
        return  finalName + (AnnotationData.hasAnnotation(genericTypeElement, DTO.class) ? DTO.class.getSimpleName() : "");
    }

    /**
     * Tries to find an "id" field inside a given enclosing type.
     * @param fieldEnclosingType a class/record that is supposed to contain the "id" field
     * @return an {@link Optional} containing "id" field if it exists; or an empty optional otherwise.
     */
    public Optional<VariableElement> findIdField(final VariableElement fieldEnclosingType) {
        return findIdField(getClassTypeElement(fieldEnclosingType));
    }

    /**
     * Checks if a given type (class/record) has an "id" field.
     * @param type the class/record type to check
     * @return an {@link Optional} containing the id field if it exists; an empty optional otherwise.
     */
    public Optional<VariableElement> findIdField(final TypeElement type) {
        final var classFieldsStream = TypeUtil.getClassFields(processor.types(), type);
        return classFieldsStream.filter(f -> f.getSimpleName().toString().equals("id")).findFirst();
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
     * Gets the fields of a given class, including the ones from its superclasses.
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     *
     * @param types        a reference to {@link DTOProcessor#types()}
     * @param classTypeElement the class to get the fields from
     * @return a stream of fields from the given class
     */
    public static Stream<VariableElement> getClassFields(final Types types, final TypeElement classTypeElement) {
        final var fieldStream =
                classTypeElement.getEnclosedElements().stream()
                                .filter(enclosedElement -> enclosedElement.getKind().isField())
                                .map(enclosedElement -> (VariableElement) enclosedElement)
                                .filter(FieldUtil::isInstanceField);

        final var superclassType = classTypeElement.getSuperclass();
        final var superclassElement = (TypeElement) types.asElement(superclassType);
        final var superClassFields = hasSuperClass(classTypeElement) ?
                                        getClassFields(types, superclassElement) :
                                        Stream.<VariableElement>empty();

        return Stream.concat(superClassFields, fieldStream);
    }

    /**
     * {@return true if a line doesn't start with a triple slash comment ///, false otherwise}
     * @param line the line to check
     */
    public static boolean isNotThreeSlashesComment(final String line) {
        return !line.trim().startsWith("///");
    }

    /**
     * Checks if a given element is a {@link Record}.
     * @param element element to check
     * @return true if a given element is a record, false otherwise
     */
    public static boolean isRecord(final Element element) {
        return element.getKind() == ElementKind.RECORD;
    }

    /**
     * Gets a {@link TypeMirror} as a {@link DeclaredType} if that {@link TypeMirror}
     * is in fact a {@link DeclaredType} (a non-primitive type).
     * @param typeMirror the type to check and get as a {@link DeclaredType}
     * @return a {@link DeclaredType} if the given type is a {@link DeclaredType}; null otherwise.
     */
    @Nullable
    public static DeclaredType getAsDeclaredType(final TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType declaredType ? declaredType : null;
    }
}
