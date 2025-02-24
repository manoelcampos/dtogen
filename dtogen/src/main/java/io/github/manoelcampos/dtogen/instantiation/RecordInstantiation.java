package io.github.manoelcampos.dtogen.instantiation;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.RecordGenerator;
import io.github.manoelcampos.dtogen.util.FieldUtil;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.stream.Stream;

/**
 * Generates the code to instantiate a record.
 * @author Manoel Campos
 * @see ClassInstantiation#newInstance(RecordGenerator, TypeElement)
 */
public final class RecordInstantiation extends ObjectInstantiation {
    RecordInstantiation(final RecordGenerator recordGen) {
        super(recordGen, ", ");
    }

    @Override
    protected String constructorCall(final String fieldValues) {
        return CODE_TEMPLATE.formatted(recordGen.getModelTypeName(), fieldValues, METHOD_CALL_CLOSING);
    }

    @Override
    protected String newObjectInternal(final String idFieldValue, final String classTypeName, final Stream<VariableElement> fieldStream) {
        return "new %s(%s)".formatted(classTypeName, generateFieldListInitialization(typeUtil, fieldStream, idFieldValue));
    }

    /**
     * {@return value to be given to a field of a model record which will be instantiated}
     * @param sourceField model field to generate the value to be passed to the class/record constructor
     */
    protected String generateFieldValueInternal(final VariableElement sourceField) {
        final boolean sourceFieldAnnotatedWithMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);
        final var fieldValue = fieldValue(sourceField, sourceFieldAnnotatedWithMapToId);
        final boolean primitive = FieldUtil.isPrimitive(sourceField);

        if(sourceFieldAnnotatedWithMapToId && !primitive) {
            final String newObjectCall = newObject(sourceField, fieldValue);

            // Instantiates an object of the type of the model field so that the id can be set
            return "%n%s".formatted(newObjectCall);
        }

        return fieldValue;
    }

    /**
     * {@inheritDoc}
     * Since this {@link RecordInstantiation} deals with a model object that is a record, a default value (according to the field type) is returned.
     *
     * @param sourceField {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected String excludedFieldValue(VariableElement sourceField) {
        return generateFieldInitialization(typeUtil, sourceField, false);
    }
}
