package io.github.manoelcampos.dtogen.samples.other;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class in a different package from {@link io.github.manoelcampos.dtogen.samples.Class11},
 * which has a collection of this class.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class12 {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
