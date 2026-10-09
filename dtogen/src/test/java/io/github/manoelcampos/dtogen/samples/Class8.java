package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;
import org.jspecify.annotations.Nullable;

/**
 * A model class with fields annotated with JSpecify's {@link Nullable},
 * which targets only {@link java.lang.annotation.ElementType#TYPE_USE}.
 * Such annotations belong to the field type, not to the field itself.
 * The class is used to check if those annotations are copied to the generated DTO record,
 * including the component generated for a {@link DTO.MapToId} field (which has a different type from the original field).
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class8 {
    private @Nullable Long id;

    private String name;

    @Nullable @DTO.MapToId
    private Class7 class7;

    public @Nullable Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable Class7 getClass7() {
        return class7;
    }

    public void setClass7(@Nullable Class7 class7) {
        this.class7 = class7;
    }
}
