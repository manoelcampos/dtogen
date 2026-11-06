package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;
import io.github.manoelcampos.dtogen.samples.other.Class12DTO;
import javax.annotation.processing.Generated;
import java.time.LocalDate;
import java.util.List;

import java.lang.reflect.Type;
import java.util.Map;

/// A sample of the DTO record that the DTOGen must generate for the [Class11] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class11}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class11DTO (Long id, List<Class12DTO> items, Map<String, List<LocalDate>> dates, Type type) implements DTORecord<Class11> {
    @Override
    public Class11 toModel(){
        final var model = new Class11();
        model.setId(id);
        model.setItems(items.stream().map(Class12DTO::toModel).toList());
        model.setDates(dates);
        model.setType(type);

        return model;
    }

    @Override
    public Class11DTO fromModel(final Class11 model){
        final var dto = new Class11DTO(
            model.getId(),
            model.getItems().stream().map(item -> new Class12DTO().fromModel(item)).toList(),
            model.getDates(),
            model.getType()
        );

        return dto;
    }

    public Class11DTO() {
        this(0L, null, null, null);
    }
}
