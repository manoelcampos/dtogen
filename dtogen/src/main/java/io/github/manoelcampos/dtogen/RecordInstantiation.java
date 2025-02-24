package io.github.manoelcampos.dtogen;

import javax.lang.model.element.VariableElement;
import java.util.stream.Stream;

/**
 * Generates the code to instantiate a record.
 * @author Manoel Campos
 */
public final class RecordInstantiation extends ObjectInstantiation {
    public RecordInstantiation(final RecordGenerator recordGen) {
        super(recordGen, ", ");
    }

    @Override
    protected String constructorCall(final String fieldValues) {
        return CODE_TEMPLATE.formatted(recordGen.getModelTypeName(), fieldValues, METHOD_CALL_CLOSING);
    }

    @Override
    protected String newObjectInternal(final String idFieldValue, final String classTypeName, final Stream<VariableElement> fieldStream) {
        return "new %s(%s)".formatted(classTypeName, recordGen.generateFieldListInitialization(fieldStream, idFieldValue));
    }

}
