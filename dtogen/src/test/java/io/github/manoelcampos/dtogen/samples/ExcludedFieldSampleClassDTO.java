package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [ExcludedFieldSampleClass] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link ExcludedFieldSampleClass}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record ExcludedFieldSampleClassDTO (boolean included) implements DTORecord<ExcludedFieldSampleClass> {
    @Override
    public ExcludedFieldSampleClass toModel(){
        final var model = new ExcludedFieldSampleClass();
        model.setIncluded(included);

        return model;
    }

    @Override
    public ExcludedFieldSampleClassDTO fromModel(final ExcludedFieldSampleClass model){
        final var dto = new ExcludedFieldSampleClassDTO(model.isIncluded());
        return dto;
    }

    public ExcludedFieldSampleClassDTO() {
        this(false);
    }
}
