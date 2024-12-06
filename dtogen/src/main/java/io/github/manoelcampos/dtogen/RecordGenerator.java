package io.github.manoelcampos.dtogen;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.manoelcampos.dtogen.AnnotationData.getFieldAnnotationsStr;
import static io.github.manoelcampos.dtogen.ClassUtil.getClassFields;
import static io.github.manoelcampos.dtogen.DTOProcessor.hasAnnotation;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Generates a {@link Record} java file from a given model class.
 * @author Manoel Campos
 */
public class RecordGenerator {
    public static final String ID_FIELD_NOT_FOUND = "Cannot find id field in %s. Since the %s.%s is annotated with %s, it must be a class with an id field.";
    private final DTOProcessor processor;

    /**
     * Name of the interface the record will implement (if any)
     */
    private final String interfaceName;

    private final String modelPackageName;
    private final String modelClassName;
    private final String recordName;

    /**
     * A predicate to indicate how annotations from a field in a model class are selected to be included
     * in the respective record field.
     */
    private final Predicate<AnnotationData> annnotationPredicate;

    /**
     * A predicate to indicate how field from a model class are selected to be included
     * in generated record.
     */
    private final Predicate<VariableElement> sourceClassFieldPredicate;

    /**
     * A Map where each <b>key</b> is a field inside a model (entity) class
     * and the corresponding <b>key value</b> is a list of the field's annotations.
     */
    private final Map<VariableElement, List<AnnotationData>> sourceFieldAnnotationsMap;


    public RecordGenerator(
        final DTOProcessor processor,
        final Element classElement, final Predicate<AnnotationData> annnotationPredicate,
        final Predicate<VariableElement> sourceClassFieldPredicate,
        final String interfaceName)
    {
        this.processor = processor;
        this.annnotationPredicate = annnotationPredicate;
        this.sourceClassFieldPredicate = sourceClassFieldPredicate;
        this.interfaceName = interfaceName;
        final var modelClassTypeElement = (TypeElement) classElement;
        this.modelPackageName = ClassUtil.getPackageName(modelClassTypeElement);
        this.modelClassName = modelClassTypeElement.getSimpleName().toString();
        this.recordName = modelClassName + "DTO";

        final var classFieldsList = getClassFields(processor.typeUtils(), modelClassTypeElement, sourceClassFieldPredicate).toList();

        // Creates a LinkedHashMap to ensure the fields are collected in the same order they are encountered in the class
        this.sourceFieldAnnotationsMap =
            classFieldsList
                .stream()
                .collect(toMap(identity(), this::getFieldAnnotations, (a, b) -> a, LinkedHashMap::new));
    }

    public RecordGenerator(
            final DTOProcessor processor,
            final Element classElement, final Predicate<AnnotationData> annnotationPredicate,
            final Predicate<VariableElement> sourceClassFieldPredicate)
    {
        this(processor, classElement, annnotationPredicate, sourceClassFieldPredicate, "");
    }

    public RecordGenerator(final DTOProcessor processor, final Element classElement, final Predicate<AnnotationData> annnotationPredicate) {
        this(processor, classElement, annnotationPredicate, field -> true, "");
    }

    public RecordGenerator(final DTOProcessor processor, final Element classElement) {
        this(processor, classElement, annotation -> true, field -> true, "");
    }

    /**
     * Generate the DTO record file for the annotated class.
     */
    public void generate() {
        final String fieldsStr = recordFieldsStr();
        final var builder = new StringBuilder();
        if (!modelPackageName.isBlank())
            builder.append("package %s;%n%n".formatted(modelPackageName));

        builder.append(fieldAnnotationsImports());
        builder.append(fieldTypeImports());

        final String implementsClause = hasInterface() ? "implements %s<%s>".formatted(interfaceName, modelClassName) : "";
        builder.append(getGeneratedAnnotation());
        builder.append("public record %s (%s) %s {%n".formatted(recordName, fieldsStr, implementsClause));
        builder.append(generateToModelMethod());
        builder.append(generateFromModelMethod());
        builder.append(defaultRecordConstrutor());
        builder.append("}%n".formatted());

        new JavaFileWriter(processor).write(modelPackageName, recordName, builder.toString());
    }

    private String getGeneratedAnnotation() {
        final var dtoProcessorClass = DTOProcessor.class.getName();
        final var comments = "DTO generated using DTOGen Annotation Processor";

        final var importAnnotation = "%nimport javax.annotation.processing.Generated;";
        final var annotation = "@Generated(value = \"%s\", comments = \"%s\")".formatted(dtoProcessorClass, comments);

        return """
                %s
                
                %s
                """.formatted(importAnnotation, annotation);
    }

    private String defaultRecordConstrutor() {
        final var builder = new StringBuilder("    public %s() {%n".formatted(recordName));
        builder.append("        this(");

        final String fieldValues =
            fieldStream()
                .map(this::generateFieldInitialization)
                .collect(joining(", "));

        builder.append(fieldValues);
        builder.append(");%n".formatted());
        builder.append("    }%n".formatted());
        return builder.toString();
    }

    /**
     * {@return  a stream of the fields of the model class being processed}
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     */
    private Stream<VariableElement> fieldStream() {
        return sourceFieldAnnotationsMap.keySet().stream();
    }

    private String generateFieldInitialization(final VariableElement sourceField) {
        final var sourceFieldTypeName = getTypeName(sourceField, false, true);
        final boolean hasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        return switch (sourceFieldTypeName) {
            case "String" -> "\"\"";
            case "Long" -> "0L";
            case "Integer", "int",  "long", "Short", "short", "Byte", "byte", "Double", "double" -> "0";
            case "Character", "char" -> "''";
            case "Boolean", "boolean" -> "false";
            default -> hasMapToId ? "0L" : "null";
        };
    }

    /**
     * {@return a string with the DTO record fields, based on the fields of the model class being processed}
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     */
    private String recordFieldsStr() {
        return sourceFieldAnnotationsMap
                .entrySet()
                .stream()
                .map(entry -> generateRecordField(entry.getKey(), entry.getValue()))
                .collect(joining(", "));
    }

    /**
     * {@return a string containing the imports for the annotations for fields from the model class}
     */
    private String fieldAnnotationsImports() {
        final var importsSet = sourceFieldAnnotationsMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(AnnotationData::name)
                .collect(toSet());

        return importsSet.stream().map("import %s;"::formatted).collect(joining("%n"));
    }

    /**
     * {@return a string containing the imports for the field types from the model class}
     */
    private String fieldTypeImports() {
        final var importsSet = sourceFieldAnnotationsMap
                .keySet()
                .stream()
                .filter(field -> !field.asType().getKind().isPrimitive())
                .filter(field -> !getTypeName(field).startsWith("java.lang."))
                .filter(field -> !isFieldClassPackageSameAsRecordPackage(getTypeName(field)))
                .map(field -> getTypeName(field, true, false))
                .collect(toSet());

        final String imports = importsSet.stream().map("import %s;"::formatted).collect(joining("%n"));
        return imports.isBlank() ? imports : System.lineSeparator() + imports;
    }

    private String getFieldName(final VariableElement field) {
        return field.getSimpleName().toString();
    }

    /**
     * @param sourceField               represents the field on the model class that will be created
     *                                  on the DTO record. Such an object enables getting field metadata.
     * @param sourceFieldAnnotationData list of annotations on the field
     * @return
     */
    private String generateRecordField(
            final VariableElement sourceField,
            final List<AnnotationData> sourceFieldAnnotationData)
    {
        final var sourceFieldAnnotationsStr = getFieldAnnotationsStr(sourceFieldAnnotationData);
        final var annotationClass = DTO.MapToId.class;
        if (!AnnotationData.contains(sourceField, annotationClass)) {
            return String.format("%s %s %s", sourceFieldAnnotationsStr, getFieldType(sourceField), sourceField.getSimpleName());
        }

        final var fieldType = processor.getClassTypeElement(sourceField);
        final var msg =
                ID_FIELD_NOT_FOUND.formatted(
                    fieldType.getSimpleName(), modelClassName,
                    sourceField.getSimpleName(), AnnotationData.getName(annotationClass)
                );

        return findIdField(fieldType)
                .map(idField -> formatIdField(sourceField, idField, sourceFieldAnnotationData))
                .orElseGet(() -> {
                    processor.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, msg, sourceField);
                    return "";
                });

    }

    private Optional<VariableElement> findIdField(final TypeElement fieldType) {
        final var classFieldsStream = getClassFields(processor.typeUtils(), fieldType, sourceClassFieldPredicate);
        return classFieldsStream.filter(f -> f.getSimpleName().toString().equals("id")).findFirst();
    }

    private String formatIdField(final VariableElement sourceField, final VariableElement idField, final List<AnnotationData> sourceFieldAnnotationData) {
        return "%s %s %sId".formatted(getFieldAnnotationsStr(sourceFieldAnnotationData), getFieldType(idField), sourceField.getSimpleName());
    }

    private String getFieldType(final VariableElement fieldElement) {
        final String typeName = getTypeName(fieldElement).replaceAll("java\\.lang\\.", "");
        return ClassUtil.getSimpleClassName(typeName);
    }

    /**
     * {@return true if the package of a field type class is the same as the package of the model class}
     * @param fullyQualifiedFieldClassName the fully qualified name of the field type class
     */
    private boolean isFieldClassPackageSameAsRecordPackage(final String fullyQualifiedFieldClassName) {
        return modelPackageName.equals(ClassUtil.getPackageName(fullyQualifiedFieldClassName));
    }

    private String getTypeName(final VariableElement fieldElement) {
        return getTypeName(fieldElement, true, true);
    }

    boolean isBooleanType(final VariableElement fieldElement) {
        return "boolean".equalsIgnoreCase(getTypeName(fieldElement));
    }

    /**
     * Gets the name of a type based on a {@link VariableElement} representing a variable/field.
     * @param fieldElement variable/field to get its type
     * @param qualified if the type name must include the full-qualified package name or just the type name
     * @param includeTypeArgs indicates if the returned type name must include possible generic type arguments
     * @return the type name
     */
    public String getTypeName(final VariableElement fieldElement, final boolean qualified, final boolean includeTypeArgs) {
        final var typeMirror = fieldElement.asType();
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            final var element = (TypeElement) declaredType.asElement();

            // Check if the type has generic parameters
            final String typeArguments = includeTypeArgs ? genericTypeArguments(declaredType) : "";
            final var name = qualified ? element.getQualifiedName() : element.getSimpleName();
            return name + typeArguments;
        }

        processor.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type: " + typeMirror, fieldElement);
        return typeMirror.toString();
    }

    /**
     * Checks if a field has a generic type argument annotated with {@link DTO}
     * in order to generate that field using the DTO record instead of the Model class.
     * Consider we have a DTO record MainDTO that has a field of type {@code List<ModelClass>}.
     * if ModelClass is created with the {@link DTO} annotation, its related field in MainDTO
     * will be {@code List<ModelClassDTO>} instead of {@code List<ModelClass>}.
     *
     * @param fieldElement the field to check
     * @return the first generic type (ModelClass from a declaration such as {@code List<ModelClass>})
     * (the generic type argument) if ModelClass is annotated with {@link DTO}; an empty string otherwise.
     */
    private String getFirstGenericTypeArgAnnotatedWithDTO(final VariableElement fieldElement) {
        final var typeMirror = fieldElement.asType();
        if (!(typeMirror instanceof DeclaredType declaredType)) {
            return "";
        }

        if (declaredType.getTypeArguments().isEmpty())
            return "";

        final var firstTypeArg = processor.getTypeMirrorAsElement(declaredType.getTypeArguments().get(0));
        return hasAnnotation(firstTypeArg, DTO.class) ? firstTypeArg.getSimpleName().toString() : "";
    }

    /**
     * Gets the generic type arguments of a given type.
     * If the type is {@code List<Customer>}, List is the declared type and Customer is the generic argument.
     *
     * @param declaredType the type to get its generic arguments
     * @return a String representing all the generic type arguments in format {@code <Type1, TypeN>} or an empty string if there are no generic type arguments.
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
        final var genericTypeElement = processor.getTypeMirrorAsElement(genericType);
        final var qualifiedName = genericTypeElement.getQualifiedName().toString();
        final var finalName = isFieldClassPackageSameAsRecordPackage(qualifiedName) ? ClassUtil.getSimpleClassName(qualifiedName) : qualifiedName;
        return  finalName + (hasAnnotation(genericTypeElement, DTO.class) ? DTO.class.getSimpleName() : "");
    }

    /**
     * {@return the annotations of a field}
     * @param field the field to get its annotations
     */
    private List<AnnotationData> getFieldAnnotations(final VariableElement field) {
        return AnnotationData.getFieldAnnotations(field, annnotationPredicate);
    }

    private boolean hasInterface() {
        return !interfaceName.isBlank();
    }

    private String generateToModelMethod() {
        final var code = """
                             @Override
                             public %s toModel(){
                                 final var model = new %s();
                         %s
                                 return model;
                             }
                         
                         """;

        final var builder = new StringBuilder();
        fieldStream()
                .map(this::generateModelSetterCall)
                .forEach(builder::append);

        return code.formatted(modelClassName, modelClassName, builder.toString());
    }

    private String generateFromModelMethod() {
        final var methodCode =
             """
                 @Override
                 public %1$s fromModel(final %2$s model){
                     final var dto = new %1$s(
             %3$s
                     );
             
                     return dto;
                 }
             
             """;

        final var constructorValues = fieldStream().map(this::dtoConstructorParam).collect(joining(",%n".formatted()));
        return methodCode.formatted(recordName, modelClassName, constructorValues);
    }

    private String dtoConstructorParam(final VariableElement sourceField) {
        final var sourceFieldName = sourceField.getSimpleName().toString();
        final var upCaseSourceFieldName = ClassUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var getterPrefix = isBooleanType(sourceField) ? "is" : "get";
        final var modelGetterName = "model.%s%s()".formatted(getterPrefix, upCaseSourceFieldName);
        if (sourceFieldHasMapToId) {
            return "          %1$s == null ? 0L : %1$s.getId()".formatted(modelGetterName);
        }

        final var genericTypeArg = getFirstGenericTypeArgAnnotatedWithDTO(sourceField);
        final var formattedGetter = "          %s".formatted(modelGetterName);

        // Generates a stream chain to map a Model object to a DTO
        final var dtoToModelMapper = ".stream().map(item -> new %s().fromModel(item)).toList()".formatted(genericTypeArg + DTO.class.getSimpleName());

        return formattedGetter + (genericTypeArg.isBlank() ? "" : dtoToModelMapper);
    }

    private String generateModelSetterCall(final VariableElement sourceField) {
        final var builder = new StringBuilder();
        final var fieldType = getTypeName(sourceField);
        final var sourceFieldName = getFieldName(sourceField);
        final var upCaseSourceFieldName = ClassUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);
        final var modelSetter =
                sourceFieldHasMapToId ?
                        "model.get%s().setId".formatted(upCaseSourceFieldName) :
                        "model.set%s".formatted(upCaseSourceFieldName);

        // Instantiates an object of the type of the model field so that the id can be set
        final var newFieldObj = sourceFieldHasMapToId ? "        model.set%s(new %s());%n".formatted(upCaseSourceFieldName, fieldType) : "";
        builder.append(newFieldObj);

        final var value = setterValue(sourceField, sourceFieldHasMapToId);
        final var setField = "        %s(this.%s);%n".formatted(modelSetter, value);
        builder.append(setField);

        return builder.toString();
    }

    /**
     * Generates the Java code representing the value to be passed to a setter method inside the {@link #generateToModelMethod()}.
     *
     * @param sourceField           the field to generate the value to be passed to the setter method
     * @param sourceFieldHasMapToId indicates if the field has the {@link DTO.MapToId} annotation.
     * @return the value to be passed to the setter method (as Java code)
     */
    private String setterValue(final VariableElement sourceField, final boolean sourceFieldHasMapToId) {
        final var sourceFieldName = getFieldName(sourceField);
        final var genericTypeArg = getFirstGenericTypeArgAnnotatedWithDTO(sourceField);
        if (genericTypeArg.isBlank()) {
            return "%s%s".formatted(sourceFieldName, sourceFieldHasMapToId ? "Id" : "");
        }

        // Generates a stream chain to map a DTO object to a Model
        return "%s.stream().map(%s::toModel).toList()".formatted(sourceFieldName, genericTypeArg + DTO.class.getSimpleName());
    }
}
