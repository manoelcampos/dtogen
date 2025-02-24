package io.github.manoelcampos.dtogen.sample.model;

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
public class CountryClass {
    // Constants aren't included in the DTO record
    private static final long SOME_CONSTANT = 1;

    /**
     *
     */
    // Since the field JavaDoc is blank, it's not copied to the generated DTO.
    @NotNull
    private long id;

    /**
     * The name of the country that has same length restrictions.
     */
    @NotNull @NotBlank @Size(min = 10, max = 200)
    private String name;

    @NotNull @NotBlank
    private String continent;

    /**
     * Since this field has a primitive type, the {@link DTO.MapToId} is not allowed,
     * therefore, it's commented out to avoid build failure.
     */
    //@DTO.MapToId
    private long areaKm2;

    /**
     * The language spoken in the country.
     * Since it's annotated with {@link DTO.MapToId}, the generated DTO class will have a field languageClassId.
     */
    @DTO.MapToId
    private LanguageClass language;
}
