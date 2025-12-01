package io.github.manoelcampos.dtogen;

import io.github.manoelcampos.dtogen.instantiation.ObjectInstantiation;
import io.github.manoelcampos.dtogen.util.FieldUtil;
import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.manoelcampos.dtogen.AnnotationData.getFieldAnnotationsStr;
import static io.github.manoelcampos.dtogen.AnnotationData.hasAnnotation;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Generates a {@link Record} java file from a given model class.
 * @author Manoel Campos
 */
public final class RecordGenerator {
    private final DTOProcessor processor;
    private static final String ln = System.lineSeparator();

    private final Set<String> excludedAnnotationNameSet = Set.of(
            DTOProcessor.class.getPackageName(),
            "jakarta.persistence.Id", "jakarta.persistence.GeneratedValue", "jakarta.persistence.Enumerated",
            "jakarta.persistence.OneToMany", "jakarta.persistence.ManyToOne",
            "jakarta.persistence.OneToOne", "jakarta.persistence.ManyToMany",
            "jakarta.persistence.JoinColumn", "jakarta.persistence.Transient", "jakarta.persistence.JoinTable",
            "jakarta.persistence.Column", "jakarta.persistence.Lob",
            "org.hibernate.annotations.",
            "javax.annotation.meta.When", "lombok", "JsonIgnore",
            DTO.class.getName()
    );

    private final String modelPackageName;

    /**
     * The type ot the model object to generate a record.
     */
    private final TypeElement modelTypeElement;
    private final String modelTypeName;
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

    /**
     * A Set with additional elements to be imported in the generated record file.
     * Each item is just the element name, not the full import statement.
     *
     * <p>Sorts the list placing javax.* imports at the beginning
     * Uses + instead of x, since + comes before x in the ASCII table.
     * The change is made locally just for sorting. The imports are not changed in the end.
     * </p>
     * @see #fieldAnnotationsImports()
     */
    private final Set<String> additionalImports = new TreeSet<>(Comparator.comparing(s -> s.replaceAll("javax", "java+")));
    private final TypeUtil typeUtil;

    public RecordGenerator(final DTOProcessor processor, final Element classElement) {
        this.processor = processor;
        this.typeUtil = processor.typeUtil();
        this.annnotationPredicate = Predicate.not(this::isExcludedAnnotation);
        this.sourceClassFieldPredicate = FieldUtil::isNotFieldExcluded;
        this.modelTypeElement = (TypeElement) classElement;
        this.modelTypeName = modelTypeElement.getSimpleName().toString();
        this.modelPackageName = TypeUtil.getPackageName(modelTypeElement);
        this.recordName = modelTypeName + "DTO";
        this.sourceFieldAnnotationsMap = newFieldsMap(modelTypeElement);
    }

    /**
     * Collects the fields of the model class and their annotations into a map.
     * Creates a LinkedHashMap to ensure the fields are collected in the same order they are encountered in the class.
     * @param modelClassTypeElement the class to get its fields and related annotations
     * @return the new fields map
     */
    private Map<VariableElement, List<AnnotationData>> newFieldsMap(final TypeElement modelClassTypeElement) {
        final var classFieldsList = TypeUtil.getClassFields(processor.types(), modelClassTypeElement).toList();
        return classFieldsList
                .stream()
                .collect(toMap(identity(), this::getFieldAnnotations, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Generates the DTO record for the {@link DTO} annotated class and writes it to a file.
     */
    public void write() {
        new JavaFileWriter(processor).write(modelPackageName, recordName, generate());
    }

    /**
     * Generates the DTO record for the {@link DTO} annotated class.
     * @return the generated java code for the DTO record
     */
    String generate() {
        final String fieldsStr = recordFieldsStr();

        final var recordBodyContent = new StringBuilder();
        final String implementsClause = "implements %s<%s>".formatted(DTORecord.class.getSimpleName(), modelTypeName);

        recordBodyContent.append(dtoJavaDoc());
        recordBodyContent.append(getGeneratedAnnotation());
        recordBodyContent.append("public record %s (%s) %s {%n".formatted(recordName, fieldsStr, implementsClause));
        recordBodyContent.append(generateToModelMethod());
        recordBodyContent.append(generateFromModelMethod());
        recordBodyContent.append(defaultRecordConstrutor());
        recordBodyContent.append("}%n".formatted());

        final var recordFullContent = new StringBuilder();
        if (!modelPackageName.isBlank())
            recordFullContent.append("package %s;%n%n".formatted(modelPackageName));

        recordFullContent.append(fieldAnnotationsImports());
        recordFullContent.append(fieldTypeImports());
        recordFullContent.append(recordBodyContent);

        return recordFullContent.toString();
    }

    /**
     * {@return the JavaDoc to be included in the generated DTO record, copying the model entity fields JavaDoc.}
     */
    private String dtoJavaDoc() {
        /* Do not include the javadoc strings as a variable for the formatted method, since that string
        may contain % characters that will be interpreted as placeholders by the method,
        causing exception during formatting.
        This way, String.format or String.formatted is not used anywhere in this method
        when JavaDoc strings are being used. */
        final var extractor = new JavaDocExtractor(modelTypeElement);
        final String fieldsJavaDoc =
                extractor.getFieldCommentsStream()
                         .map(e -> " * @param " + e.getKey() + " " +  e.getValue().replaceAll("\n", " "))
                         .collect(joining(ln));

        final var recordJavaDoc = """
                /**
                 * A {@link DTORecord Data Transfer Object} for {@link %s}."""
                .formatted(modelTypeName);
        return recordJavaDoc + (fieldsJavaDoc.isBlank() ? "" : " %n * %n".formatted() + fieldsJavaDoc) + ln + " */" + ln;
    }

    private String getGeneratedAnnotation() {
        final var processorClass = DTOProcessor.class.getName();
        final var comments = "DTO generated using DTOGen Annotation Processor";

        addElementToImport("javax.annotation.processing.Generated");
        return "@Generated(value = \"%s\", comments = \"%s\")%n".formatted(processorClass, comments);
    }

    private String defaultRecordConstrutor() {
        final var builder = new StringBuilder("    public %s() {%n".formatted(recordName));
        builder.append("        this(");

        final String fieldValues = generateFieldListInitialization();
        builder.append(fieldValues);
        builder.append(");%n".formatted());
        builder.append("    }%n".formatted());
        return builder.toString();
    }

    /**
     * {@return a string with a default value for a given stream of fields, according to each field type}
     */
    public String generateFieldListInitialization() {
        return ObjectInstantiation.generateFieldListInitialization(typeUtil, fieldStream(), null);
    }

    /**
     * {@return  a stream of the fields of the model class being processed}
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     */
    Stream<VariableElement> fieldStream() {
        return fieldStream(sourceClassFieldPredicate);
    }

    public Stream<VariableElement> allFieldsStream() {
        return fieldStream(field -> true);
    }

    private Stream<VariableElement> fieldStream(final Predicate<VariableElement> fieldPredicate) {
        return sourceFieldAnnotationsMap.keySet().stream().filter(fieldPredicate);
    }

    /**
     * {@return a string with the DTO record fields, based on the fields of the model class being processed}
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     */
    private String recordFieldsStr() {
        return sourceFieldAnnotationsMap
                .entrySet()
                .stream()
                .filter(entry -> sourceClassFieldPredicate.test(entry.getKey()))
                .map(entry -> generateRecordField(entry.getKey(), entry.getValue()))
                .collect(joining(", "));
    }

    /**
     * {@return a String representing a field for the DTO record being generated}
     * @param sourceField the field on the model class that will be created
     *                    on the DTO record. Such an object enables getting field metadata.
     * @param sourceFieldAnnotationData list of annotations on the field
     */
    private String generateRecordField(
        final VariableElement sourceField,
        final List<AnnotationData> sourceFieldAnnotationData)
    {
        final var sourceFieldAnnotationsStr = getFieldAnnotationsStr(sourceFieldAnnotationData);
        final var mapToIdAnnotation = DTO.MapToId.class;
        final var fieldClassType = processor.typeUtil().getTypeElement(sourceField);
        final boolean primitive = fieldClassType == null;
        final boolean containsMapToId = AnnotationData.contains(sourceField, mapToIdAnnotation);

        if(containsMapToId && primitive){
            final var msg = "The @MapToId annotation in %s is not allowed for primitive fields.";
            processor.error(sourceField, msg.formatted(sourceField.getSimpleName()));
        }

        if (!containsMapToId || primitive) {
            return String.format("%s %s %s", sourceFieldAnnotationsStr, getFieldType(sourceField), sourceField.getSimpleName());
        }

        final var msg =
                FieldUtil.ID_FIELD_NOT_FOUND.formatted(
                        fieldClassType.getSimpleName(), modelTypeName,
                        sourceField.getSimpleName(), AnnotationData.getName(mapToIdAnnotation)
                );

        return typeUtil.findIdField(fieldClassType)
                .map(idField -> formatIdField(sourceField, idField, sourceFieldAnnotationData))
                .orElseGet(() -> {
                    processor.error(sourceField, msg);
                    return "";
                });
    }

    /**
     * {@return a String containing all the imports for the annotations from the model class fields}
     * That list includes the {@link #additionalImports}.
     */
    private String fieldAnnotationsImports() {
        final var importsSet = sourceFieldAnnotationsMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(AnnotationData::name)
                .collect(toSet());

        final var importStream = Stream.concat(importsSet.stream(), additionalImports.stream());
        final String imports = importStream.map("import %s;"::formatted).collect(joining(ln)) + ln;
        return "%s%n".formatted(imports);
    }

    /**
     * {@return a string containing the imports for the field types from the model class}
     */
    private String fieldTypeImports() {
        final var importsSet = sourceFieldAnnotationsMap
                .keySet()
                .stream()
                .filter(field -> !FieldUtil.isPrimitive(field))
                .filter(field -> !typeUtil.getTypeName(field).startsWith("java.lang."))
                .filter(field -> !isFieldTypePkgSameAsRecordPkg(typeUtil.getTypeName(field)))
                .map(field -> typeUtil.getTypeName(field, true, false))
                .collect(toSet());

        final String imports = importsSet.stream().map("import %s;"::formatted).collect(joining(ln));
        return imports.isBlank() ? imports : ln + imports;
    }

    private String formatIdField(final VariableElement sourceField, final VariableElement idField, final List<AnnotationData> sourceFieldAnnotationData) {
        return "%s %s %sId".formatted(getFieldAnnotationsStr(sourceFieldAnnotationData), getFieldType(idField), sourceField.getSimpleName());
    }

    /**
     * {@return the type of a field without the java.lang package prefix (if existing)}
     * @param fieldElement field element to get its type
     */
    private String getFieldType(final VariableElement fieldElement) {
        final String typeName = typeUtil.getTypeName(fieldElement).replaceAll("java\\.lang\\.", "");
        return TypeUtil.getSimpleClassName(typeName);
    }

    /**
     * {@return true if the package of a field type is the same as the package of the model class}
     * @param fullyQualifiedFieldClassName the fully qualified name of the field type class
     */
    private boolean isFieldTypePkgSameAsRecordPkg(final String fullyQualifiedFieldClassName) {
        return isFieldTypePkgEqualsTo(fullyQualifiedFieldClassName, modelPackageName);
    }

    /**
     * {@return true if the package of a field type is equal to a given package}
     *
     * @param fullyQualifiedFieldTypeName the fully qualified name of the field type
     * @param packageName                 the name of the package to check if the field type belongs to
     */
    public static boolean isFieldTypePkgEqualsTo(final String fullyQualifiedFieldTypeName, final String packageName) {
        return packageName.equals(TypeUtil.getPackageName(fullyQualifiedFieldTypeName));
    }

    boolean isBooleanType(final VariableElement fieldElement) {
        return "boolean".equalsIgnoreCase(typeUtil.getTypeName(fieldElement));
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
    public String getFirstGenericTypeArgAnnotatedWithDTO(final VariableElement fieldElement) {
        final var typeMirror = fieldElement.asType();
        if (!(typeMirror instanceof DeclaredType declaredType)) {
            return "";
        }

        if (declaredType.getTypeArguments().isEmpty())
            return "";

        final var firstTypeArg = typeUtil.getTypeMirrorAsTypeElement(declaredType.getTypeArguments().getFirst());
        return hasAnnotation(firstTypeArg, DTO.class) ? firstTypeArg.getSimpleName().toString() : "";
    }



    /**
     * {@return the annotations of a field}
     * @param field the field to get its annotations
     */
    private List<AnnotationData> getFieldAnnotations(final VariableElement field) {
        return AnnotationData.getFieldAnnotations(field, annnotationPredicate);
    }

    private String generateToModelMethod() {
        final var template = """
                                 @Override
                                 public %s toModel(){
                                 %s
                                 }
                             
                             """;

        allFieldsStream()
                .filter(field -> AnnotationData.contains(field, DTO.MapToId.class) && !FieldUtil.isPrimitive(field))
                .forEach(field -> addElementToImport(typeUtil.getTypeName(field, true, false)));

        // formatted() replaces %n codes by the OS-dependent char
        final var methodInternalCode = ObjectInstantiation.newInstance(this, modelTypeElement).generate().formatted();
        return template.formatted(modelTypeName, methodInternalCode);
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
        return methodCode.formatted(recordName, modelTypeName, constructorValues);
    }

    /**
     * Generates the value representing a parameter for a DTO constructor call.
     * @param sourceField the field in the model/entity class to pass as parameter to the DTO constructor
     * @return a String representing the generated value to pass to the constructor
     */
    private String dtoConstructorParam(final VariableElement sourceField) {
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var modelGetterName = "model." + getterName(sourceField);
        // If declaredType is null, the field type is primitive and the MapToId annotation is not allowed
        final var fieldDeclaredType = TypeUtil.getAsDeclaredType(sourceField.asType());
        if (sourceFieldHasMapToId && fieldDeclaredType != null) {
            // Default value for a numeric field (according to its type)
            final String defaultNumVal = ObjectInstantiation.generateFieldInitialization(typeUtil, sourceField, null);
            final String idFieldValue =
                    typeUtil.findIdField(sourceField)
                            .map(idField -> "          %1$s == null ? %2$s : %1$s.%3$s".formatted(modelGetterName, defaultNumVal, getterName(idField)))
                            .orElse("");

            // If there is no "id" field, ignores the MapToId annotation
            if (!idFieldValue.isBlank()) {
                return idFieldValue;
            }
        }

        final var genericTypeArg = getFirstGenericTypeArgAnnotatedWithDTO(sourceField);
        final var formattedGetter = "          %s".formatted(modelGetterName);

        // Generates a stream chain to map a Model object to a DTO
        final var dtoRecordName = genericTypeArg + DTO.class.getSimpleName();
        final var dtoToModelMapper = ".stream().map(item -> new %s().fromModel(item)).toList()".formatted(dtoRecordName);

        return formattedGetter + (genericTypeArg.isBlank() ? "" : dtoToModelMapper);
    }

    /**
     * {@return the name of the getter for a given field in a model/entity class}
     * @param sourceField field to obtain its getter name
     */
    private String getterName(final VariableElement sourceField) {
        final var sourceFieldName = sourceField.getSimpleName().toString();
        final boolean isRecord = TypeUtil.isRecord(sourceField.getEnclosingElement());
        final var formatedFieldName = isRecord ? sourceFieldName : FieldUtil.getUpCaseFieldName(sourceFieldName);

        final var prefixForGetterInsideClass = isBooleanType(sourceField) ? "is" : "get";
        final var prefix = isRecord ? "" : prefixForGetterInsideClass;
        return "%s%s()".formatted(prefix, formatedFieldName);
    }

    /**
     * Adds an elemento to the {@link #additionalImports} list
     * and returns the simple name of that element.
     * @param elementQualifiedName full qualified name (including package) of the element to add to the imports list
     */
    private void addElementToImport(final String elementQualifiedName) {
        final String elementPackage = TypeUtil.getPackageName(elementQualifiedName);
        if(!elementPackage.equals(modelPackageName) && !elementPackage.isBlank() && !elementQualifiedName.startsWith("java.lang"))
            additionalImports.add(elementQualifiedName);
    }

    /**
     * Checks if an annotation is to be excluded from the DTO fields.
     * DTO or a JPA/Hibernation annotations has only effect on database tables and
     * must not be included in the DTO record.
     *
     * @param annotation the annotation to check
     * @return true if the annotation is to be excluded, false otherwise
     */
    private boolean isExcludedAnnotation(final AnnotationData annotation) {
        return excludedAnnotationNameSet.stream().anyMatch(annotation.name()::contains);
    }

    public DTOProcessor getProcessor() {
        return processor;
    }

    public TypeElement getModelTypeElement() {
        return modelTypeElement;
    }

    public String getModelTypeName() {
        return modelTypeName;
    }
}
