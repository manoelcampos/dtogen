package io.github.manoelcampos.dtogen.instantiation;

import io.github.manoelcampos.dtogen.RecordGenerator;

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
}
