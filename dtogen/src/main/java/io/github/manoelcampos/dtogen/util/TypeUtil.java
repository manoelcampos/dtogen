package io.github.manoelcampos.dtogen.util;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.DTOProcessor;
import io.github.manoelcampos.dtogen.RecordGenerator;

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
     * Checks if a given type is a primitive type.
     * @param type the type to check
     * @return true if it's primitive, false otherwise
     */
    public static boolean isPrimitive(final TypeMirror type) {
        return type.getKind().isPrimitive();
    }

    /**
     * {@return a TypeElement that represents a field type, or null if the field is primitive}
     * @param fieldElement the element representing the field.
     */
    public @Nullable TypeElement getFieldTypeElement(final VariableElement fieldElement) {
        return getTypeMirrorAsTypeElement(fieldElement.asType());
    }

    public TypeElement getTypeMirrorAsTypeElement(final TypeMirror typeMirror) {
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
        final var declaredType = getAsDeclaredType(typeMirror);
        if (declaredType == null) // is primitive
            return typeMirror.toString().toLowerCase();

        final var typeElement = (TypeElement) declaredType.asElement();
        // Check if the type has generic parameters
        final String typeArguments = includeTypeArgs ? genericTypeArguments(fieldElement, declaredType) : "";
        final var name = qualified ? typeElement.getQualifiedName() : typeElement.getSimpleName();
        return name + typeArguments;
    }

    /**
     * Gets the generic type arguments of a given type.
     * If the type is {@code List<Customer>}, List is the declared type and Customer is the generic argument.
     *
     * @param fieldElement a field to get the generic arguments from some of its types
     * @param declaredType a type from the field declaration to get its generic arguments.
     *                     The type can be from any part of the field declaration.
     *                     Considering the field type as {@code Map<String, List<Double>>},
     *                     the method can get the generic types from {@code Map}
     *                     or from the inner type {@code List<Double>}.
     * @return a String representing all the generic type arguments from the declaredType in format {@code <Type1, TypeN>};
     *         or an empty string if there are no generic type arguments or the field type is primitive.
     */
    private String genericTypeArguments(final VariableElement fieldElement, final DeclaredType declaredType) {
        if(declaredType == null) // primitive type
            return "";

        final var typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return "";
        }

        // Recursively gets the generic type arguments for each generic type argument.
        final var genericTypeArgs =
                typeArguments
                        .stream()
                        .map(typeArg -> getGenericTypeArgTypes(fieldElement, typeArg))
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
     * @param fieldElement the field to get some generic type args
     * @param genericTypeArg the generic type argument to get its own type arguments (if any)
     * @return a string representation of the generic type argument with its own type arguments (if any)
     */
    private String getGenericTypeArgTypes(final VariableElement fieldElement, final TypeMirror genericTypeArg) {
        final var declaredGenericTypeArg = (DeclaredType) genericTypeArg;
        // Gets the generic types of the generic type arg
        final var genericSubTypes = genericTypeArguments(fieldElement, declaredGenericTypeArg);
        return "%s%s".formatted(genericTypeArgument(fieldElement, genericTypeArg), genericSubTypes);
    }

    /**
     * @see #genericTypeArguments(VariableElement, DeclaredType)
     */
    private String genericTypeArgument(final VariableElement fieldElement, final TypeMirror genericType) {
        final var fieldPackageName = getPackageName(fieldElement.getEnclosingElement());
        final var genericTypeElement = getTypeMirrorAsTypeElement(genericType);
        final var qName = genericTypeElement.getQualifiedName().toString();
        final var finalName = RecordGenerator.isFieldTypePkgEqualsTo(qName, fieldPackageName) ? getSimpleClassName(qName) : qName;
        return  finalName + (AnnotationData.hasAnnotation(genericTypeElement, DTO.class) ? DTO.class.getSimpleName() : "");
    }

    /**
     * Tries to find an "id" field inside a given enclosing type.
     * @param fieldEnclosingType a class/record that is supposed to contain the "id" field
     * @return an {@link Optional} containing "id" field if it exists; or an empty optional otherwise.
     */
    public Optional<VariableElement> findIdField(final VariableElement fieldEnclosingType) {
        return findIdField(getFieldTypeElement(fieldEnclosingType));
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
     * {@return the package name from the type of a given element (variable/field)}
     * @param element the element to get the package name from its type
     */
    private String getPackageName(final Element element) {
        return getPackageName(getTypeMirrorAsTypeElement(element.asType()).getQualifiedName().toString());
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
     * @return a {@link DeclaredType} if the given type is a {@link DeclaredType}; null if it's a primitive type.
     */
    @Nullable
    public static DeclaredType getAsDeclaredType(final TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType declaredType ? declaredType : null;
    }
}
