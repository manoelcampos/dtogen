package io.github.manoelcampos.dtogen;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static io.github.manoelcampos.dtogen.AnnotationData.getFieldAnnotationsStr;
import static io.github.manoelcampos.dtogen.ClassUtil.getClassFields;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Generates a {@link Record} java file.
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

    private final TypeElement modelClassTypeElement;

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
        this.modelClassTypeElement = (TypeElement) classElement;
        this.modelPackageName = ClassUtil.getPackageName(modelClassTypeElement);
        this.modelClassName = modelClassTypeElement.getSimpleName().toString();
        this.recordName = modelClassName + "DTO";

        final var classFieldsList = getClassFields(processor.typeUtils(), modelClassTypeElement, sourceClassFieldPredicate).toList();
        this.sourceFieldAnnotationsMap = classFieldsList.stream().collect(toMap(identity(), this::getFieldAnnotations));
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
        processor.processingEnv().getMessager().printMessage(Diagnostic.Kind.NOTE, recordName, classElement);

        final var builder = new StringBuilder();
        if(!modelPackageName.isBlank())
            builder.append("package %s;%n%n".formatted(modelPackageName));

        final String implementsClause = hasInterface() ? "implements %s<%s>".formatted(interfaceName, modelClassName) : "";
        builder.append("public record %s (%s) %s {%n".formatted(recordName, fieldsStr, implementsClause));
        builder.append(generateToModelMethod());
        builder.append("}%n".formatted());

        new JavaFileWriter(processor).write(modelPackageName, recordName, builder.toString());
    }

    /**
     * {@return a string with the DTO record fields} It is based on the fields of the annotated class.
     */
    private String recordFieldsStr() {
        return sourceFieldAnnotationsMap
                .entrySet()
                .stream()
                .map(entry -> createRecordField(entry.getKey(), entry.getValue()))
                .collect(joining(", "));
    }

    /**
     * @param sourceField               represents the field on the model class that will be created
     *                                  on the DTO record. Such an object enables getting field metadata.
     * @param sourceFieldAnnotationData list of annotations on the field
     * @return
     */
    private String createRecordField(
            final VariableElement sourceField,
            final List<AnnotationData> sourceFieldAnnotationData)
    {
        final var sourceFieldAnnotationsStr = getFieldAnnotationsStr(sourceFieldAnnotationData);
        final var annotationClass = DTO.MapToId.class;
        if(AnnotationData.contains(sourceField, annotationClass)){
            final var fieldType = getClassTypeElement(sourceField);
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

        return String.format("%s %s %s", sourceFieldAnnotationsStr, getFieldType(sourceField), sourceField.getSimpleName());
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
        final var typeMirror = fieldElement.asType();
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            final var element = (TypeElement) declaredType.asElement();

            // Check if the type has generic parameters
            final String typeArguments = genericTypeArguments(declaredType);
            return element.getQualifiedName().toString() + typeArguments;
        }

        processor.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type: " + typeMirror, fieldElement);
        return typeMirror.toString();
    }

    private static String genericTypeArguments(final DeclaredType declaredType) {
        final var typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return "";
        }

        final var genericTypes = typeArguments.stream().map(TypeMirror::toString).collect(joining(", "));
        return "<" + genericTypes + ">";
    }

    /**
     * Gets the class that represents the field type.
     * @param fieldElement the element representing the field.
     * @return
     */
    private TypeElement getClassTypeElement(final VariableElement fieldElement) {
        final var fieldTypeMirror = fieldElement.asType();
        return (TypeElement) processor.typeUtils().asElement(fieldTypeMirror);
    }

    /**
     * Gets the annotations of a field.
     * @param field the field to get its annotations
     * @return
     */
    private List<AnnotationData> getFieldAnnotations(final VariableElement field) {
        return AnnotationData.getFieldAnnotations(field, annnotationPredicate);
    }

    private boolean hasInterface(){
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
        sourceFieldAnnotationsMap
                .keySet()
                .stream()
                .map(this::generateSetterCall)
                .forEach(builder::append);

        return code.formatted(modelClassName, modelClassName, builder.toString());
    }

    private String generateSetterCall(final VariableElement sourceField) {
        final var fieldType = getTypeName(sourceField);
        final var sourceFieldName = sourceField.getSimpleName().toString();
        final var sourceUpCaseFieldName = ClassUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);
        final var modelSetter =
                sourceFieldHasMapToId ?
                        "model.get%s().setId".formatted(sourceUpCaseFieldName) :
                        "model.set%s".formatted(sourceUpCaseFieldName);

        // Instantiates an object of the type of the model field so that the id can be set
        final var newFieldObj = sourceFieldHasMapToId ? "        model.set%s(new %s());%n".formatted(sourceUpCaseFieldName, fieldType) : "";

        final var value = "%s%s".formatted(sourceFieldName, sourceFieldHasMapToId ? "Id" : "");
        final var setField = "        %s(this.%s);%n".formatted(modelSetter, value);
        return newFieldObj + setField;
    }
}
