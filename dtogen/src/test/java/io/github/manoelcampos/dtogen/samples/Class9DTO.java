package io.github.manoelcampos.dtogen.samples;

import jakarta.validation.constraints.Pattern;
import io.github.manoelcampos.dtogen.DTORecord;

import javax.annotation.processing.Generated;

/// A sample of the DTO record that the DTOGen must generate for the [Class9] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class9}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class9DTO (@Pattern(regexp="[A-Z]{3}\\d{4}") String plate, @Pattern(regexp=".*", message="say \"hi\"\n\ttab") String text, Double price, Float weight, float height, Short stock, Byte rating) implements DTORecord<Class9> {
    @Override
    public Class9 toModel(){
        final var model = new Class9();
        model.setPlate(plate);
        model.setText(text);
        model.setPrice(price);
        model.setWeight(weight);
        model.setHeight(height);
        model.setStock(stock);
        model.setRating(rating);

        return model;
    }

    @Override
    public Class9DTO fromModel(final Class9 model){
        final var dto = new Class9DTO(
            model.getPlate(),
            model.getText(),
            model.getPrice(),
            model.getWeight(),
            model.getHeight(),
            model.getStock(),
            model.getRating()
        );

        return dto;
    }

    public Class9DTO() {
        this("", "", 0.0, 0.0F, 0.0F, (short) 0, (byte) 0);
    }
}
