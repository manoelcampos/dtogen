package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class2] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class2}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class2DTO (long id, int class3Id) implements DTORecord<Class2> {
    @Override
    public Class2 toModel(){
        final var model = new Class2();
        model.setId(id);
        model.setClass3(newObject(class3Id, () -> { var o = new Class3(); o.setId(class3Id); return o; }));

        return model;
    }

    @Override
    public Class2DTO fromModel(final Class2 model){
        final var dto = new Class2DTO(
                model.getId(),
                model.getClass3() == null ? 0 : model.getClass3().getId()
        );

        return dto;
    }

    public Class2DTO() {
        this(0, 0);
    }
}
