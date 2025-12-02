package io.github.manoelcampos.dtogen.sample.model;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.*;

/**
 * @author Manoel Campos
 * @param country The country where the person is currently living.
 */
@DTO
public record PersonRecord(
        long id,
        @NotNull @NotBlank String name,
        @DecimalMin("0.1") @DecimalMax("200") double weightKg,
        @Min(5) @Max(50) int footSize,
        @DTO.Exclude String password,
        @DTO.MapToId CountryClass country,
        @DTO.MapToId ProfessionRecord profession,
        ReligionClass religion,
        PersonRecord mother,
        @DTO.MapToId PersonRecord father)
{
    public PersonRecord(long id, String name, double weightKg, int footSize, String password, CountryClass country, ProfessionRecord profession, ReligionClass religion) {
        this(id, name, weightKg, footSize, password, country, profession, religion, null, null);
    }
}
