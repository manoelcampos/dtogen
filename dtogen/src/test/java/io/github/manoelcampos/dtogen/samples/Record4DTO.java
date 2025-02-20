package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Record4] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Record4DTO (Long id,  double width,  long class1Id) implements DTORecord<Record4> {
    @Override
    public Record4 toModel(){
        final var model = new Record4(
                id, width,
                newObject(class1Id, () -> { var o = new Class1(); o.setId(class1Id); return o; })
        );

        return model;
    }

    @Override
    public Record4DTO fromModel(final Record4 model){
        final var dto = new Record4DTO(
                model.id(),
                model.width(),
                model.class1() == null ? 0 : model.class1().getId()
        );

        return dto;
    }

    public Record4DTO() {
        this(0L, 0, 0);
    }
}
