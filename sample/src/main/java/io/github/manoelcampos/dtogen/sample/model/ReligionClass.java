package io.github.manoelcampos.dtogen.sample.model;

import io.github.manoelcampos.dtogen.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Manoel Campos
 */
@Data @AllArgsConstructor @NoArgsConstructor @DTO
public class ReligionClass {
    private String description;
    private String origin;
}
