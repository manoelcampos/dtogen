package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A sample model class annotated with {@link DTO}.
 * @author Manoel Campos
 */
@DTO
public class Class3 {
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
