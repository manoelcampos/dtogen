package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class6] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class6}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class6DTO (long id, long class5Id) implements DTORecord<Class6> {
    @Override
    public Class6 toModel(){
        final var model = new Class6();
        model.setId(id);
        model.setClass5(newObject(class5Id, () -> { var o = new Class5(); o.setId(class5Id); return o; }));

        return model;
    }

    @Override
    public Class6DTO fromModel(final Class6 model){
        final var dto = new Class6DTO(
                model.getId(),
                model.getClass5() == null ? 0 : model.getClass5().getId()
        );

        return dto;
    }

    public Class6DTO() {
        this(0, 0);
    }
}
