package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;
import java.util.List;

/// A sample of the DTO record that the DTOGen must generate for the [SampleClass] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are striped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link SampleClass}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record SampleClassDTO (String str,  boolean bool,  List<String> genericList,  List nonGenericList) implements DTORecord<SampleClass> {
    @Override
    public SampleClass toModel(){
        final var model = new SampleClass();
        model.setStr(str);
        model.setBool(bool);
        model.setGenericList(genericList);
        model.setNonGenericList(nonGenericList);

        return model;
    }

    @Override
    public SampleClassDTO fromModel(final SampleClass model){
        final var dto = new SampleClassDTO(
            model.getStr(),
            model.isBool(),
            model.getGenericList(),
            model.getNonGenericList()
        );

        return dto;
    }

    public SampleClassDTO() {
        this("", false, null, null);
    }
}
