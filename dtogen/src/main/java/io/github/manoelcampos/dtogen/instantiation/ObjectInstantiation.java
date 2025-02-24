package io.github.manoelcampos.dtogen.instantiation;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.DTOProcessor;
import io.github.manoelcampos.dtogen.RecordGenerator;
import io.github.manoelcampos.dtogen.util.FieldUtil;
import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;
import java.util.stream.Stream;

import static io.github.manoelcampos.dtogen.util.FieldUtil.isNotIdField;
import static java.util.stream.Collectors.joining;

/**
 * Generates the code to instantiate an object, whose type is a class or record,
 * using the values from a model class/record object.
 * @author Manoel Campos
 *
 * @see #newInstance(RecordGenerator, TypeElement)
 */
public abstract sealed class ObjectInstantiation permits ClassInstantiation, RecordInstantiation{
    protected static final String CODE_TEMPLATE =
                         """
                                 final var model = new %s(%s
                         %s
                                 return model;""";
    protected static final String METHOD_CALL_CLOSING = ");";

    /**
     * A {@link RecordGenerator} instance for the DTO record being generated,
     * which indicates the class/record of the model object to
     */
    protected final RecordGenerator recordGen;
    protected final TypeUtil typeUtil;
    private final DTOProcessor processor;

    /**
     * A delimiter to be used to separate the value for each field during the object instantiation.
     * If the object is a record, the delimiter is a comma (since all values are given to the record constructor);
     * if it's a class, the delimiter is a new line (since each value is given calling a specific setter).
     */
    private final String fieldDelimiter;

    /**
     * An internal constructor for subclasses.
     * @param recordGen see {@link #recordGen}
     * @param fieldDelimiter see {@link #fieldDelimiter}
     * @see #newInstance(RecordGenerator, TypeElement)
     */
    ObjectInstantiation(final RecordGenerator recordGen, final String fieldDelimiter) {
        this.recordGen = Objects.requireNonNull(recordGen);
        this.processor = recordGen.getProcessor();
        this.fieldDelimiter = Objects.requireNonNull(fieldDelimiter);
        this.typeUtil = processor.typeUtil();
    }

    /**
     * {@return the actual code to call a constructor to instantiate an object using the values from a model object}
     * The model object to be instantiate is defined by the {@link RecordGenerator#getModelTypeElement()}.
     * @see #recordGen
     */
    public final String generate(){
        // Gets all fields in the model class to allow instantiating it (including fields annotated with @DTO.Ignore)
        final String fieldValues =
                recordGen.allFieldsStream()
                         .map(this::generateFieldValue)
                         .collect(joining(fieldDelimiter));

        return constructorCall(fieldValues);
    }

    /**
     * {@return value to be given to a field of a model class/record which will be instantiated}
     * @param sourceField model field to generate the value to be passed to the class/record constructor
     */
    String generateFieldValue(final VariableElement sourceField) {
        final var builder = new StringBuilder();
        final var sourceFieldName = FieldUtil.getFieldName(sourceField);
        final var upCaseSourceFieldName = FieldUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldAnnotatedWithMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var setterValue = setterValue(sourceField, sourceFieldAnnotatedWithMapToId);
        final var isEnclosingElementRecord = TypeUtil.isRecord(sourceField.getEnclosingElement());

        final String modelSetIdCall;
        final boolean primitive = FieldUtil.isPrimitive(sourceField);
        if(setterValue.isBlank()){
            return "";
        }

        if(sourceFieldAnnotatedWithMapToId && !primitive) {
            modelSetIdCall = "";
            final String fieldValue = newObject(sourceField, setterValue);

            // Instantiates an object of the type of the model field so that the id can be set
            final var modelSetterCall = "          model.set%s(%s);%n".formatted(upCaseSourceFieldName, fieldValue);
            builder.append(isEnclosingElementRecord ? "%n%s".formatted(fieldValue) : modelSetterCall);
        }
        else modelSetIdCall = "model.set%s".formatted(upCaseSourceFieldName);

        if(!modelSetIdCall.isBlank()) {
            final String value = "        %s(this.%s);".formatted(modelSetIdCall, setterValue);
            builder.append(isEnclosingElementRecord ? setterValue : value);
        }

        return builder.toString();
    }

    /**
     * Generates the Java code representing the value to be passed to a setter method call.
     *
     * @param sourceField           the field to generate the value to be passed to the setter method
     * @param sourceFieldHasMapToId indicates if the field has the {@link DTO.MapToId} annotation.
     * @return the value to be passed to the setter method (as Java code)
     */
    private String setterValue(final VariableElement sourceField, final boolean sourceFieldHasMapToId) {
        if(FieldUtil.isFieldExcluded(sourceField))
            return TypeUtil.isRecord(sourceField.getEnclosingElement()) ? generateFieldInitialization(typeUtil, sourceField, false) : "";

        final var sourceFieldName = FieldUtil.getFieldName(sourceField);
        final var genericTypeArg = recordGen.getFirstGenericTypeArgAnnotatedWithDTO(sourceField);
        final boolean notPrimitive = !FieldUtil.isPrimitive(sourceField);
        if (genericTypeArg.isBlank()) {
            return "%s%s".formatted(sourceFieldName, sourceFieldHasMapToId && notPrimitive ? "Id" : "");
        }

        // Generates a stream chain to map a DTO object to a Model
        return "%s.stream().map(%s::toModel).toList()".formatted(sourceFieldName, genericTypeArg + DTO.class.getSimpleName());
    }

    /**
     * Generates the actual code to call a constructor to instantiate a model object.
     * @param fieldValues the list os values for the object attributes to be used to instantiate it
     * @return the generated constructor call code
     */
    protected abstract String constructorCall(String fieldValues);

    /**
     * Generates the code to instantiate a model object from the type of given field.
     * @param sourceField field to get its class/record type to instantiate an object
     * @param idFieldValue the value for the id field
     * @return the generated constructor call code
     */
    public final String newObject(final VariableElement sourceField, final String idFieldValue) {
        final var fieldTypeName = typeUtil.getTypeName(sourceField, false, true);
        final var fieldTypeElement = typeUtil.getFieldTypeElement(sourceField);
        final var isPrimitive = fieldTypeElement == null;

        // If the field is primitive, return the default value for that type (since there is no instantiation for such types).
        if(isPrimitive)
            return generateFieldInitialization(typeUtil, sourceField, false);

        final var fieldStream = TypeUtil.getClassFields(processor.types(), fieldTypeElement);

        // Since the field type may be either a class or record, we need a new ObjectInstantiation according to the field type
        final var fieldInstantiation = newInstance(recordGen, typeUtil.getFieldTypeElement(sourceField));
        return fieldInstantiation.newObjectInternal(idFieldValue, fieldTypeName, fieldStream);
    }

    protected abstract String newObjectInternal(String idFieldValue, String classTypeName, Stream<VariableElement> fieldStream);

    /**
     * {@return a string with a default value for a given stream of fields, according to each field type}
     * @param fieldStream  stream of field to generate their initialization value
     * @param idFieldValue a value for the id field (null if the default value must be used, according to the field type)
     */
    public static String generateFieldListInitialization(final TypeUtil typeUtil, final Stream<VariableElement> fieldStream, @Nullable final Object idFieldValue) {
        return fieldStream
                .map(field -> generateFieldInitialization(typeUtil, field, idFieldValue))
                .collect(joining(", "));
    }

    /**
     * {@return a string with a default value for a given stream of fields, according to each field type}
     * @param sourceField  the field to generate the initialization value for
     * @param idFieldValue a value for the id field (null if the default value must be used, according to the field type)
     */
    public static String generateFieldInitialization(final TypeUtil typeUtil, final VariableElement sourceField, final Object idFieldValue) {
        // If no value is given for the id field, the field initialization list is used to instantiate a DTO object with all default values.
        final boolean dtoInstantiation = idFieldValue == null;
        return isNotIdField(sourceField) || idFieldValue == null ? generateFieldInitialization(typeUtil, sourceField, dtoInstantiation) : idFieldValue.toString();
    }

    /**
     * {@return a default value for a given field, according to its type}
     * @param sourceField field to generate the initialization value
     * @param dtoInstantiation true to indicate the field initialization values will be used to instantiate
     *                         a DTO object, false to indicate a model/entity object will be instantiated
     */
    static String generateFieldInitialization(final TypeUtil typeUtil, final VariableElement sourceField, final boolean dtoInstantiation) {
        final var sourceFieldTypeName = typeUtil.getTypeName(sourceField, false, false);
        final boolean hasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        return switch (sourceFieldTypeName) {
            case "String" -> "\"\"";
            case "Long" -> "0L";
            case "Integer", "int",  "long", "Short", "short", "Byte", "byte", "Double", "double" -> "0";
            case "Character", "char" -> "'\\0'";
            case "Boolean", "boolean" -> "false";
            default -> {
                final String value = typeUtil.findIdField(sourceField)
                                             .map(idField -> generateFieldInitialization(typeUtil, idField, dtoInstantiation))
                                             .orElse("0L");

                yield hasMapToId && dtoInstantiation ? value : "null";
            }
        };
    }

    public static ObjectInstantiation newInstance(final RecordGenerator gen, final TypeElement modelTypeElement){
        return TypeUtil.isRecord(modelTypeElement) ? new RecordInstantiation(gen) : new ClassInstantiation(gen);
    }
}
