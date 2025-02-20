package io.github.manoelcampos.dtogen;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.manoelcampos.dtogen.AnnotationData.getFieldAnnotationsStr;
import static io.github.manoelcampos.dtogen.ClassUtil.*;
import static io.github.manoelcampos.dtogen.DTOProcessor.hasAnnotation;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Generates a {@link Record} java file from a given model class.
 * @author Manoel Campos
 */
public class RecordGenerator {
    private static final String ID_FIELD_NOT_FOUND = "Cannot find id field in %s. Since the %s.%s is annotated with %s, it must be a class with an id field.";
    private final DTOProcessor processor;

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
     * The type ot the model class to generate a record.
     */
    private final TypeElement modelClassTypeElement;
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

    /**
     * A Set with additional elements to be imported in the generated record file.
     * Each item is just the element name, not the full import statement.
     *
     * <p>Sorts the list placing javax.* imports at the beginning
     * Uses + instead of x since + comes before x in the ASCII table.
     * The change is made locally just for sorting. The imports are not changed in the end.
     * </p>
     * @see #fieldAnnotationsImports()
     */
    private final Set<String> additionalImports = new TreeSet<>(Comparator.comparing(s -> s.replaceAll("javax", "java+")));

    public RecordGenerator(final DTOProcessor processor, final Element classElement) {
        this.processor = processor;
        this.annnotationPredicate = Predicate.not(this::isExcludedAnnotation);
        this.sourceClassFieldPredicate = RecordGenerator::isNotFieldExcluded;
        this.modelClassTypeElement = (TypeElement) classElement;
        this.modelClassName = modelClassTypeElement.getSimpleName().toString();
        this.modelPackageName = ClassUtil.getPackageName(modelClassTypeElement);
        this.recordName = modelClassName + "DTO";
        this.sourceFieldAnnotationsMap = newFieldsMap(modelClassTypeElement);
    }

    /**
     * Collects the fields of the model class and their annotations into a map.
     * Creates a LinkedHashMap to ensure the fields are collected in the same order they are encountered in the class.
     * @param modelClassTypeElement the class to get its fields and related annotations
     * @return the new fields map
     */
    private Map<VariableElement, List<AnnotationData>> newFieldsMap(final TypeElement modelClassTypeElement) {
        final var classFieldsList = getClassFields(processor.typeUtils(), modelClassTypeElement).toList();
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
        final String implementsClause = "implements %s<%s>".formatted(DTORecord.class.getSimpleName(), modelClassName);
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

        // Replaces %n codes by the OS-dependent char
        return String.format(recordFullContent.toString());
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
    private String generateFieldListInitialization() {
        return generateFieldListInitialization(fieldStream(), null);
    }

    /**
     * {@return a string with a default value for a given stream of fields, according to each field type}
     * @param fieldStream  stream of field to generate their initialization value
     * @param idFieldValue a value for the id field (null if the default value must be used, according to the field type)
     */
    private String generateFieldListInitialization(final Stream<VariableElement> fieldStream, @Nullable final Object idFieldValue) {
        // If no value is given for the id field, the field initialization list is used to instantiate a DTO object with all default values.
        final boolean dtoInstantiation = idFieldValue == null;
        return fieldStream
                .map(field -> isNotIdField(field) || idFieldValue == null ? generateFieldInitialization(field, dtoInstantiation) : idFieldValue.toString())
                .collect(joining(", "));
    }

    private boolean isNotIdField(final VariableElement field) {
        return !"id".equals(getFieldName(field));
    }

    /**
     * {@return  a stream of the fields of the model class being processed}
     * It doesn't sort elements to ensure the fields are returned in the same order they are declared in the class.
     */
    private Stream<VariableElement> fieldStream() {
        return fieldStream(sourceClassFieldPredicate);
    }

    private Stream<VariableElement> fieldStream(final Predicate<VariableElement> fieldPredicate) {
        return sourceFieldAnnotationsMap.keySet().stream().filter(fieldPredicate);
    }

    /**
     * {@return a default value for a given field, according to its type}
     * @param sourceField field to generate the initialization value
     * @param dtoInstantiation true to indicate the field initialization values will be used to instantiate
     *                         a DTO object, false to indicate a model/entity object will be instantiated
     */
    private String generateFieldInitialization(final VariableElement sourceField, final boolean dtoInstantiation) {
        final var sourceFieldTypeName = getTypeName(sourceField, false, true);
        final boolean hasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        return switch (sourceFieldTypeName) {
            case "String" -> "\"\"";
            case "Long" -> "0L";
            case "Integer", "int",  "long", "Short", "short", "Byte", "byte", "Double", "double" -> "0";
            case "Character", "char" -> "'\\0'";
            case "Boolean", "boolean" -> "false";
            default -> {
                final String value = findIdField(sourceField)
                        .map(idField -> generateFieldInitialization(idField, dtoInstantiation))
                        .orElse("0L");

                yield hasMapToId && dtoInstantiation ? value : "null";
            }
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
                .filter(entry -> sourceClassFieldPredicate.test(entry.getKey()))
                .map(entry -> generateRecordField(entry.getKey(), entry.getValue()))
                .collect(joining(", "));
    }

    /**
     * {@return a String representing a field for the DTO record being generated}
     * @param sourceField the field on the model class that will be created
     *                    on the DTO record. Such an object enables getting field metadata.
     * @param sourceFieldAnnotationData list of annotations on the field
     *
     */
    private String generateRecordField(
            final VariableElement sourceField,
            final List<AnnotationData> sourceFieldAnnotationData)
    {
        final var sourceFieldAnnotationsStr = getFieldAnnotationsStr(sourceFieldAnnotationData);
        final var mapToIdAnnotation = DTO.MapToId.class;
        final var fieldClassType = processor.getClassTypeElement(sourceField);
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
                ID_FIELD_NOT_FOUND.formatted(
                        fieldClassType.getSimpleName(), modelClassName,
                        sourceField.getSimpleName(), AnnotationData.getName(mapToIdAnnotation)
                );

        return findIdField(fieldClassType)
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
        final String imports = importStream.map("import %s;"::formatted).collect(joining("%n")) + System.lineSeparator();
        return "%s%n".formatted(imports);
    }

    /**
     * {@return a string containing the imports for the field types from the model class}
     */
    private String fieldTypeImports() {
        final var importsSet = sourceFieldAnnotationsMap
                .keySet()
                .stream()
                .filter(field -> !isPrimitive(field))
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
     * Tries to find an "id" field inside a given enclosing type.
     * @param fieldEnclosingType a class/record that is supposed to contain the "id" field
     * @return an {@link Optional} containing "id" field if it exists; or an empty optional otherwise.
     */
    private Optional<VariableElement> findIdField(final VariableElement fieldEnclosingType) {
        return findIdField(processor.getClassTypeElement(fieldEnclosingType));
    }

    /**
     * Checks if a given type (class/record) has an "id" field.
     * @param type the class/record type to check
     * @return an {@link Optional} containing the id field if it exists; an empty optional otherwise.
     */
    private Optional<VariableElement> findIdField(final TypeElement type) {
        final var classFieldsStream = getClassFields(processor.typeUtils(), type);
        return classFieldsStream.filter(f -> f.getSimpleName().toString().equals("id")).findFirst();
    }

    private String formatIdField(final VariableElement sourceField, final VariableElement idField, final List<AnnotationData> sourceFieldAnnotationData) {
        return "%s %s %sId".formatted(getFieldAnnotationsStr(sourceFieldAnnotationData), getFieldType(idField), sourceField.getSimpleName());
    }

    /**
     * {@return the type of a field without the java.lang package prefix (if existing)}
     * @param fieldElement field element to get its type
     */
    private String getFieldType(final VariableElement fieldElement) {
        final String typeName = getTypeName(fieldElement).replaceAll("java\\.lang\\.", "");
        return ClassUtil.getSimpleClassName(typeName);
    }

    /**
     * {@return true if the package of a field type is the same as the package of the model class}
     * @param fullyQualifiedFieldClassName the fully qualified name of the field type class
     */
    private boolean isFieldClassPackageSameAsRecordPackage(final String fullyQualifiedFieldClassName) {
        return modelPackageName.equals(ClassUtil.getPackageName(fullyQualifiedFieldClassName));
    }

    boolean isBooleanType(final VariableElement fieldElement) {
        return "boolean".equalsIgnoreCase(getTypeName(fieldElement));
    }

    public String getTypeName(final VariableElement fieldElement) {
        return getTypeName(fieldElement, true, true);
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
        }

        final var declaredType = getAsDeclaredType(typeMirror);
        if (declaredType == null) {
            processor.error(fieldElement, "Unsupported type: " + typeMirror);
            return typeMirror.toString();
        }

        final var element = (TypeElement) declaredType.asElement();
        // Check if the type has generic parameters
        final String typeArguments = includeTypeArgs ? genericTypeArguments(declaredType) : "";
        final var name = qualified ? element.getQualifiedName() : element.getSimpleName();
        return name + typeArguments;
    }

    /**
     * Gets a {@link TypeMirror} as a {@link DeclaredType} if that {@link TypeMirror}
     * is in fact a {@link DeclaredType} (a type that represents a record, class, interface...).
     * @param typeMirror the type to check and get as a {@link DeclaredType}
     * @return a {@link DeclaredType} if the given type is a {@link DeclaredType}; null otherwise.
     */
    @Nullable DeclaredType getAsDeclaredType(final TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType declaredType ? declaredType : null;
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

        final var firstTypeArg = processor.getTypeMirrorAsElement(declaredType.getTypeArguments().getFirst());
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

    private String generateToModelMethod() {
        final var code = """
                             @Override
                             public %s toModel(){
                                 final var model = new %s(%s
                         %s
                                 %s
                         
                                 return model;
                             }
                         
                         """;

        final boolean record = isRecord(modelClassTypeElement);
        // Gets all fields in the model class to allow instantiating it (including fields annotated with @DTO.Ignore)
        final String fieldValues =
                fieldStream(field -> true)
                    .map(this::generateValueForModelField)
                    .collect(joining(isRecord(modelClassTypeElement) ? ", " : "%n"));

        // If it's a record, the constructor call closing doesn't happen at the beginning of the call, but at the end (afer all parameters)
        final var beginningClosing = record ? "" : ");";

        // If it's a class, the constructor call closing happens right at the same line of the method call (since we are calling the no-args constructor)
        final var endingClosing = record ? ");" : "";
        return code.formatted(modelClassName, modelClassName, beginningClosing, fieldValues, endingClosing);
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

    /**
     * Generates the value representing a parameter for a DTO constructor call.
     * @param sourceField the field in the model/entity class to pass as parameter to the DTO constructor
     * @return a String representing the generated value to pass to the constructor
     */
    private String dtoConstructorParam(final VariableElement sourceField) {
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var modelGetterName = "model." + getterName(sourceField);
        // If declaredType is null, the field type is primitive and the MapToId annotation is not allowed
        final var fieldDeclaredType = getAsDeclaredType(sourceField.asType());
        if (sourceFieldHasMapToId && fieldDeclaredType != null) {
            // Default value for a numeric field (according to its type)
            final String defaultNumVal = generateFieldInitialization(sourceField, true);
            final String idFieldValue =
                    findIdField(sourceField)
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
        final boolean isRecord = isRecord(sourceField.getEnclosingElement());
        final var formatedFieldName = isRecord ? sourceFieldName : ClassUtil.getUpCaseFieldName(sourceFieldName);

        final var prefixForGetterInsideClass = isBooleanType(sourceField) ? "is" : "get";
        final var prefix = isRecord ? "" : prefixForGetterInsideClass;
        return "%s%s()".formatted(prefix, formatedFieldName);
    }

    /**
     * Checks if a given element is a {@link Record}.
     * @param element element to check
     * @return true if a given element is a record, false otherwise
     */
    private static boolean isRecord(final Element element) {
        return element.getKind() == ElementKind.RECORD;
    }

    /**
     * {@return value to be given to a field of a model/entity class which will be instantiated}
     * @param sourceField model field to generate the value to be passed to the class/record constructor
     */
    private String generateValueForModelField(final VariableElement sourceField) {
        final var builder = new StringBuilder();
        final var sourceFieldName = getFieldName(sourceField);
        final var upCaseSourceFieldName = ClassUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldHasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var setterValue = setterValue(sourceField, sourceFieldHasMapToId);
        final var isRecord = isRecord(sourceField.getEnclosingElement());

        final String modelSetId;
        final boolean primitive = isPrimitive(sourceField);
        if(setterValue.isBlank()){
            return "";
        }

        if(sourceFieldHasMapToId && !primitive) {
            modelSetId = "";
            final String fieldValue = newObject(sourceField, setterValue);

            // Instantiates an object of the type of the model field so that the id can be set
            final var modelSetterCall = "          model.set%s(%s);%n".formatted(upCaseSourceFieldName, fieldValue);
            builder.append(isRecord ? "%n%s".formatted(fieldValue) : modelSetterCall);
        }
        else modelSetId = "model.set%s".formatted(upCaseSourceFieldName);

        if(!modelSetId.isBlank()) {
            final String value = "        %s(this.%s);".formatted(modelSetId, setterValue);
            builder.append(isRecord ? setterValue : value);
        }

        return builder.toString();
    }

    /**
     * Generates the code to instantiate a model object from the type of given field.
     * @param sourceField field to get its class/record type to instantiate an object
     * @param idFieldValue the value for the id field
     * @return the generated constructor call code
     */
    private String newObject(final VariableElement sourceField, final String idFieldValue) {
        final var classTypeName = addElementToImport(getTypeName(sourceField));;
        final var classTypeElement = processor.getClassTypeElement(sourceField);
        final var isPrimitive = classTypeElement == null;

        // If the field is primitive, return the default value for that type (since there is no instantiation for such types).
        if(isPrimitive)
            return generateFieldInitialization(sourceField, false);

        final var fieldStream = getClassFields(processor.typeUtils(), classTypeElement);
        final var recordInstantiation = "new %s(%s)".formatted(classTypeName, generateFieldListInitialization(fieldStream, idFieldValue));
        final var classInstantiation = " newObject(%s, () -> { var o = new %s(); o.setId(%s); return o; })".formatted(idFieldValue, classTypeName, idFieldValue);
        return classTypeElement.getKind() == ElementKind.RECORD ?
                recordInstantiation :
                classInstantiation;
    }

    /**
     * Adds an elemento to the {@link #additionalImports} list
     * and returns the simple name of that element.
     * @param elementQualifiedName full qualified name (including package) of the element to add to the imports list
     * @return the simple name of the element.
     */
    private String addElementToImport(final String elementQualifiedName) {
        final String elementPackage = getPackageName(elementQualifiedName);
        if(!elementPackage.equals(modelPackageName) && !elementPackage.isBlank() && !elementQualifiedName.startsWith("java.lang"))
            additionalImports.add(elementQualifiedName);

        return getSimpleClassName(elementQualifiedName);
    }

    /**
     * Generates the Java code representing the value to be passed to a setter method inside the {@link #generateToModelMethod()}.
     *
     * @param sourceField           the field to generate the value to be passed to the setter method
     * @param sourceFieldHasMapToId indicates if the field has the {@link DTO.MapToId} annotation.
     * @return the value to be passed to the setter method (as Java code)
     */
    private String setterValue(final VariableElement sourceField, final boolean sourceFieldHasMapToId) {
        if(isFieldExcluded(sourceField))
            return isRecord(sourceField.getEnclosingElement()) ? generateFieldInitialization(sourceField, false) : "";

        final var sourceFieldName = getFieldName(sourceField);
        final var genericTypeArg = getFirstGenericTypeArgAnnotatedWithDTO(sourceField);
        final boolean notPrimitive = !isPrimitive(sourceField);
        if (genericTypeArg.isBlank()) {
            return "%s%s".formatted(sourceFieldName, sourceFieldHasMapToId && notPrimitive ? "Id" : "");
        }

        // Generates a stream chain to map a DTO object to a Model
        return "%s.stream().map(%s::toModel).toList()".formatted(sourceFieldName, genericTypeArg + DTO.class.getSimpleName());
    }

    /**
     * Annotations to be excluded from the DTO fields.
     * Check if an annotation is a DTO one or a JPA/Hibernation annotation
     * that has only effect on database tables and should not be included in the DTO record.
     * @param annotation the annotation to check
     * @return true if the annotation is a JPA/Hibernation annotation, false otherwise.
     */
    private boolean isExcludedAnnotation(final AnnotationData annotation) {
        return excludedAnnotationNameSet.stream().anyMatch(annotation.name()::contains);
    }

    /**
     * {@return true if a field must not be excluded from the generated DTO record, false otherwise}
     * @param field the field to check
     */
    static boolean isNotFieldExcluded(final VariableElement field) {
        return !isFieldExcluded(field);
    }

    /**
     * {@return true if a field must be excluded from the generated DTO record, false otherwise}
     * @param field the field to check
     */
    static boolean isFieldExcluded(final VariableElement field) {
        return hasAnnotation(field, DTO.Exclude.class);
    }
}
