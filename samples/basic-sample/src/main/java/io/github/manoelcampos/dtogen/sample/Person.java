package io.github.manoelcampos.dtogen.sample;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Manoel Campos
 */
@DTO @Data @AllArgsConstructor @NoArgsConstructor
public class Person implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;

    @NotNull @NotBlank
    private String name;

    @DecimalMin("0.1") @DecimalMax("200")
    private double weightKg;

    @Min(5) @Max(50)
    private int footSize;

    @DTO.IgnoreField
    private String password;

    @DTO.MapIdOnly
    private Country country;

    private Profession profession;
}
