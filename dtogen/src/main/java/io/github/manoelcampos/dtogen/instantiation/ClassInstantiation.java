package io.github.manoelcampos.dtogen.instantiation;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.RecordGenerator;
import io.github.manoelcampos.dtogen.util.FieldUtil;
import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.stream.Stream;

/**
 * Generates the code to instantiate a class.
 * @author Manoel Campos
 * @see ClassInstantiation#newInstance(RecordGenerator, TypeElement)
 */
public final class ClassInstantiation extends ObjectInstantiation {
    ClassInstantiation(final RecordGenerator recordGen) {
        super(recordGen, "%n");
    }

    @Override
    protected String constructorCall(final String fieldValues) {
        return CODE_TEMPLATE.formatted(recordGen.getModelTypeName(), METHOD_CALL_CLOSING, fieldValues);
    }

    @Override
    protected String newObjectInternal(final String idFieldValue, final String classTypeName, final Stream<VariableElement> __) {
        return " newObject(%s, () -> { var o = new %s(); o.setId(%s); return o; })".formatted(idFieldValue, classTypeName, idFieldValue);
    }

    /**
     * {@return value to be given to a field of a model class/record which will be instantiated}
     * Depending on whether the object being instantiated is a record or class,
     * the value is given to the constructor or to a specific setter method, respectively.
     * @param sourceField model field to generate the value to be passed to the class/record constructor
     */
    protected String generateFieldValueInternal(final VariableElement sourceField) {
        final var builder = new StringBuilder();
        final var sourceFieldName = FieldUtil.getFieldName(sourceField);
        final var upCaseSourceFieldName = FieldUtil.getUpCaseFieldName(sourceFieldName);
        final boolean sourceFieldAnnotatedWithMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);

        final var fieldValue = fieldValue(sourceField, sourceFieldAnnotatedWithMapToId);
        final var isEnclosingElementRecord = TypeUtil.isRecord(sourceField.getEnclosingElement());

        final String modelSetIdCall;
        final boolean primitive = FieldUtil.isPrimitive(sourceField);
        if(fieldValue.isBlank()){
            return "";
        }

        if(sourceFieldAnnotatedWithMapToId && !primitive) {
            modelSetIdCall = "";
            final String newObjectCall = newObject(sourceField, fieldValue);

            // Instantiates an object of the type of the model field so that the id can be set
            final var modelSetterCall = "          model.set%s(%s);%n".formatted(upCaseSourceFieldName, newObjectCall);
            builder.append(isEnclosingElementRecord ? "%n%s".formatted(newObjectCall) : modelSetterCall);
        }
        else modelSetIdCall = "model.set%s".formatted(upCaseSourceFieldName);

        if(!modelSetIdCall.isBlank()) {
            final String value = "        %s(this.%s);".formatted(modelSetIdCall, fieldValue);
            builder.append(isEnclosingElementRecord ? fieldValue : value);
        }

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Since this {@link ClassInstantiation} deals with a model object that is a class,
     * a setter call would be needed to define a value to the model object being instantiated.
     * Considering there is no value to passed to the setter, an empty string is returned to indicate the
     * setter call must be ignored.</p>
     *
     * @param sourceField {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected String excludedFieldValue(final VariableElement sourceField) {
        return "";
    }

}
