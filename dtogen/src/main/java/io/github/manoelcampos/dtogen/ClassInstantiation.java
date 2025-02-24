package io.github.manoelcampos.dtogen;

import javax.lang.model.element.VariableElement;
import java.util.stream.Stream;

/**
 * Generates the code to instantiate a class.
 * @author Manoel Campos
 */
public final class ClassInstantiation extends ObjectInstantiation {
    public ClassInstantiation(final RecordGenerator recordGen) {
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
