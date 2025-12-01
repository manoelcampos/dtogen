package io.github.manoelcampos.dtogen.sample.model;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

/**
 * A class with all public fields and no getters/setters.
 * @author Manoel Campos
 */
@DTO @NoArgsConstructor
public class ContinentClass {
    @Min(1)
    public long id;

    @NotNull @NotBlank @Size(min = 10, max = 200)
    public String name;

    public ContinentClass(final String name) {
        this.name = name;
    }
}
