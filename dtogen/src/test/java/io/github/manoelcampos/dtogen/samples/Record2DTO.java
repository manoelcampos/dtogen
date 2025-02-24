package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;
import java.time.LocalTime;

/// A sample of the DTO record that the DTOGen must generate for the [Record2] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Record2}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Record2DTO ( Long id,  LocalTime time,  double value,  Long record3Id) implements DTORecord<Record2> {
    @Override
    public Record2 toModel(){
        final var model = new Record2(id, time, value, new Record3(record3Id, '\0'));
        return model;
    }

    @Override
    public Record2DTO fromModel(final Record2 model){
        final var dto = new Record2DTO(
                model.id(),
                model.time(),
                model.value(),
                model.record3() == null ? 0L : model.record3().id()
        );

        return dto;
    }

    public Record2DTO() {
        this(0L, null, 0, 0L);
    }
}
