package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;
import org.jspecify.annotations.Nullable;

/**
 * A model class used as the target of a {@link DTO.MapToId} association in {@link Class8}.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class7 {
    private @Nullable Long id;

    public @Nullable Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }
}
