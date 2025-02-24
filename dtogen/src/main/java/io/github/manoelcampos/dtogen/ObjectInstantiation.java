package io.github.manoelcampos.dtogen;

import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * Generates the code to instantiate an object from a class or record.
 * @author Manoel Campos
 */
public abstract sealed class ObjectInstantiation permits ClassInstantiation, RecordInstantiation{
    protected static final String CODE_TEMPLATE =
                         """
                                 final var model = new %s(%s
                         %s
                                 return model;
                         """;
    protected static final String METHOD_CALL_CLOSING = ");";

    protected final RecordGenerator recordGen;
    private final TypeUtil typeUtil;
    private final DTOProcessor processor;

    /**
     * A delimiter to be used to separate the value for each field during the object instantiation.
     * If the object is a record, the delimiter is a comma (since all values are given to the record constructor);
     * if it's a class, the delimiter is a new line (since each value is given calling a specific setter).
     */
    private final String fieldDelimiter;

    public ObjectInstantiation(final RecordGenerator recordGen, final String fieldDelimiter) {
        this.recordGen = Objects.requireNonNull(recordGen);
        this.processor = recordGen.getProcessor();
        this.fieldDelimiter = Objects.requireNonNull(fieldDelimiter);
        this.typeUtil = processor.typeUtil();
    }

    /**
     * {@return the actual code to call a constructor to instantiate a model object}
     * The model object to be instantiate is defined by the {@link RecordGenerator#getModelTypeElement()}.
     * @see #recordGen
     */
    public final String generate(){
        // Gets all fields in the model class to allow instantiating it (including fields annotated with @DTO.Ignore)
        final String fieldValues =
                recordGen.allFieldsStream()
                         .map(recordGen::generateValueForModelField)
                         .collect(joining(fieldDelimiter));

        return constructorCall(fieldValues);
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
        final var classTypeName = typeUtil.getTypeName(sourceField, false, true);
        final var classTypeElement = typeUtil.getClassTypeElement(sourceField);
        final var isPrimitive = classTypeElement == null;

        // If the field is primitive, return the default value for that type (since there is no instantiation for such types).
        if(isPrimitive)
            return generateFieldInitialization(sourceField, false);

        final var fieldStream = TypeUtil.getClassFields(processor.types(), classTypeElement);
        return newObjectInternal(idFieldValue, classTypeName, fieldStream);
    }

    protected abstract String newObjectInternal(String idFieldValue, String classTypeName, Stream<VariableElement> fieldStream);

    /**
     * {@return a default value for a given field, according to its type}
     * @param sourceField field to generate the initialization value
     * @param dtoInstantiation true to indicate the field initialization values will be used to instantiate
     *                         a DTO object, false to indicate a model/entity object will be instantiated
     */
    private String generateFieldInitialization(final VariableElement sourceField, final boolean dtoInstantiation) {
        final var sourceFieldTypeName = typeUtil.getTypeName(sourceField, false, true);
        final boolean hasMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        return switch (sourceFieldTypeName) {
            case "String" -> "\"\"";
            case "Long" -> "0L";
            case "Integer", "int",  "long", "Short", "short", "Byte", "byte", "Double", "double" -> "0";
            case "Character", "char" -> "'\\0'";
            case "Boolean", "boolean" -> "false";
            default -> {
                final String value = typeUtil.findIdField(sourceField)
                        .map(idField -> generateFieldInitialization(idField, dtoInstantiation))
                        .orElse("0L");

                yield hasMapToId && dtoInstantiation ? value : "null";
            }
        };
    }



    public static ObjectInstantiation newInstance(final RecordGenerator gen, final TypeElement modelTypeElement){
        return TypeUtil.isRecord(modelTypeElement) ? new RecordInstantiation(gen) : new ClassInstantiation(gen);
    }
}
