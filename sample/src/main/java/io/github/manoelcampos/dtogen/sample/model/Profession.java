package io.github.manoelcampos.dtogen.sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Manoel Campos
 */
@Data @AllArgsConstructor @NoArgsConstructor
public class Profession {
    private long id;
    private String name;
    private String description;
}
