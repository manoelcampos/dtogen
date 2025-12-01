package io.github.manoelcampos.dtogen.instantiation;

import io.github.manoelcampos.dtogen.AnnotationData;
import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.RecordGenerator;
import io.github.manoelcampos.dtogen.util.AccessorMethod;
import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.stream.Stream;

import static io.github.manoelcampos.dtogen.util.AccessorMethod.AccessorType;

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
    protected String newObjectInternal(final Element fieldClass, final String idFieldValue, final Stream<VariableElement> __) {
        // Class to be instantiated (the class of the field to assign the new object to)
        final var classTypeName = fieldClass.getSimpleName().toString();
        final var setter = TypeUtil.getPublicMethod(fieldClass, "setId");
        final var fieldAssignment = (setter.isPresent() ? "setId(%s)" : "id = %s").formatted(idFieldValue);
        return " newObject(%s, () -> { var o = new %s(); o.%s; return o; })".formatted(idFieldValue, classTypeName, fieldAssignment);
    }

    /**
     * {@return value to be given to a field of a model class/record which will be instantiated}
     * Depending on whether the object being instantiated is a record or class,
     * the value is given to the constructor or to a specific setter method, respectively.
     * @param sourceField model field to generate the value to be passed to the class/record constructor
     */
    protected String generateFieldValueInternal(final VariableElement sourceField) {
        final boolean sourceFieldAnnotatedWithMapToId = AnnotationData.contains(sourceField, DTO.MapToId.class);
        final var fieldValue = fieldValue(sourceField, sourceFieldAnnotatedWithMapToId);

        if(fieldValue.isBlank()){
            return "";
        }

        final var accessor = new AccessorMethod(typeUtil, sourceField, AccessorType.SETTER);
        // Calls the setter of assign the field directly if there is no setter
        final var fieldAccess = "          model." + (accessor.existing() ? "%s(%s)" : "%s = %s") + ";";
        if(sourceFieldAnnotatedWithMapToId && !accessor.isPrimitiveField()) {
            final String newObjectCall = newObject(sourceField, fieldValue);

            // Instantiates an object of the type of the model field so that the id can be set
            return fieldAccess.formatted(accessor.methodOrField(), newObjectCall) + "%n";
        }

        return fieldAccess.formatted(accessor.methodOrField(), fieldValue);
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
