package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;
import java.time.LocalDate;


/// A sample of the DTO record that the DTOGen must generate for the [Record1] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Record1}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Record1DTO (Long id, String name, LocalDate date) implements DTORecord<Record1> {
    @Override
    public Record1 toModel(){
        final var model = new Record1(id, name, date);
        return model;
    }

    @Override
    public Record1DTO fromModel(final Record1 model){
        final var dto = new Record1DTO(
                model.id(),
                model.name(),
                model.date()
        );

        return dto;
    }

    public Record1DTO() {
        this(0L, "", null);
    }
}
