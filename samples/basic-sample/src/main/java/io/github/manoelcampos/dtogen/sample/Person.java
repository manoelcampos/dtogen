package io.github.manoelcampos.dtogen.sample;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Manoel Campos
 */
@DTO @Data @AllArgsConstructor @NoArgsConstructor
public class Person {
    // Constants aren't included in the DTO record
    private static final long serialVersionUID = 1L;

    private long id;

    @NotNull @NotBlank
    private String name;

    @DecimalMin("0.1") @DecimalMax("200")
    private double weightKg;

    @Min(5) @Max(50)
    private int footSize;

    @DTO.Exclude
    private String password;

    @DTO.MapToId
    private Country country;

    private Profession profession;
}
