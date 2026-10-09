package io.github.manoelcampos.dtogen.samples;

import org.jspecify.annotations.Nullable;
import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class8] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class8}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class8DTO (@Nullable() Long id, String name, @Nullable() Long class7Id) implements DTORecord<Class8> {
    @Override
    public Class8 toModel(){
        final var model = new Class8();
        model.setId(id);
        model.setName(name);
        model.setClass7(newObject(class7Id, () -> { var o = new Class7(); o.setId(class7Id); return o; }));

        return model;
    }

    @Override
    public Class8DTO fromModel(final Class8 model){
        final var dto = new Class8DTO(
                model.getId(),
                model.getName(),
                model.getClass7() == null ? 0L : model.getClass7().getId()
        );

        return dto;
    }

    public Class8DTO() {
        this(0L, "", 0L);
    }
}
