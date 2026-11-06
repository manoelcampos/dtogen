package io.github.manoelcampos.dtogen.samples.other;

import io.github.manoelcampos.dtogen.DTORecord;
import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class12] model.
/// It's required to compile the [io.github.manoelcampos.dtogen.samples.Class11DTO],
/// which has a collection of this DTO.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class12}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class12DTO (Long id) implements DTORecord<Class12> {
    @Override
    public Class12 toModel(){
        final var model = new Class12();
        model.setId(id);

        return model;
    }

    @Override
    public Class12DTO fromModel(final Class12 model){
        final var dto = new Class12DTO(
            model.getId()
        );

        return dto;
    }

    public Class12DTO() {
        this(0L);
    }
}
