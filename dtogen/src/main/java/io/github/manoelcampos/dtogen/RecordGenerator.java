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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

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

    /**
     * The model class from which the record will be generated.
     */
    private final Element classElement;

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
    private final Map<VariableElement, List<AnnotationData>> sourceFieldAnnotationsMap;
    private final Comparator<VariableElement> fieldNameComparator = Comparator.comparing(this::getFieldName);


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
        this.classElement = classElement;
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
        this(processor, classElement, anotation -> true, field -> true, "");
    }

    /**
     * Generate the DTO record file for the annotated class.
     */
    public void generate() {
        final String fieldsStr = recordFieldsStr();
        final var builder = new StringBuilder();
        if (!modelPackageName.isBlank())
            builder.append("package %s;%n%n".formatted(modelPackageName));

        final String implementsClause = hasInterface() ? "implements %s<%s>".formatted(interfaceName, modelClassName) : "";
        builder.append("public record %s (%s) %s {%n".formatted(recordName, fieldsStr, implementsClause));
        builder.append(generateToModelMethod());
        builder.append(generateFromModelMethod());
        builder.append(defaultRecordConstrutor());
        builder.append("}%n".formatted());

        new JavaFileWriter(processor).write(modelPackageName, recordName, builder.toString());
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
        final var sourceFieldTypeName = getTypeName(sourceField, false);
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
        return getTypeName(fieldElement).replaceAll("java\\.lang\\.", "");
    }

    private String getTypeName(final VariableElement fieldElement) {
        return getTypeName(fieldElement, true);
    }

    /**
     * Gets the name of a type based on a {@link VariableElement} representing a variable/field.
     * @param fieldElement variable/field to get its type
     * @param qualified if the type name must include the full-qualified package name or just the type name
     * @return the type name
     */
    private String getTypeName(final VariableElement fieldElement, final boolean qualified) {
        final var typeMirror = fieldElement.asType();
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            final var element = (TypeElement) declaredType.asElement();

            // Check if the type has generic parameters
            final String typeArguments = genericTypeArguments(declaredType);
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

        final var genericTypes = typeArguments.stream().map(this::genericTypeArgument).collect(joining(", "));
        return "<" + genericTypes + ">";
    }

    /**
     * @see #genericTypeArguments(DeclaredType)
     */
    private String genericTypeArgument(final TypeMirror genericType) {
        final var genericTypeElement = processor.getTypeMirrorAsElement(genericType);
        return genericTypeElement.getQualifiedName() + (hasAnnotation(genericTypeElement, DTO.class) ? DTO.class.getSimpleName() : "");
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

        final var modelGetterName = "model.get%s()".formatted(upCaseSourceFieldName);
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
