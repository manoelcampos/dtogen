package io.github.manoelcampos.dtogen.samples;

import org.jspecify.annotations.Nullable;
import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class7] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
/// It's also required to compile the [Class10DTO], which has collections of this DTO.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class7}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class7DTO (@Nullable() Long id) implements DTORecord<Class7> {
    @Override
    public Class7 toModel(){
        final var model = new Class7();
        model.setId(id);

        return model;
    }

    @Override
    public Class7DTO fromModel(final Class7 model){
        final var dto = new Class7DTO(
            model.getId()
        );

        return dto;
    }

    public Class7DTO() {
        this(0L);
    }
}
