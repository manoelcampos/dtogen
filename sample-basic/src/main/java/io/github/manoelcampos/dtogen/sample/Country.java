package io.github.manoelcampos.dtogen.sample;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Manoel Campos
 */
@DTO @Data @AllArgsConstructor @NoArgsConstructor
public class Country {
    @NotNull
    private long id;

    @NotNull @NotBlank @Size(min = 10, max = 200)
    private String name;

    @NotNull @NotBlank
    private String continent;

    private long areaKm2;
}
