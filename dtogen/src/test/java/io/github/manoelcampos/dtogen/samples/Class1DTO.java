package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class1] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class1}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class1DTO (long id,  Class2 class2) implements DTORecord<Class1> {
    @Override
    public Class1 toModel(){
        final var model = new Class1();
        model.setId(id);
        model.setClass2(class2);

        return model;
    }

    @Override
    public Class1DTO fromModel(final Class1 model){
        final var dto = new Class1DTO(
                model.getId(),
                model.getClass2()
        );

        return dto;
    }

    public Class1DTO() {
        this(0, null);
    }
}
