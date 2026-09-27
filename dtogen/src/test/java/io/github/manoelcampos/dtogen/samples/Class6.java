package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class containing an association with {@link Class5} annotated with {@link DTO.MapToId},
 * where {@link Class5} has no setter of its own for the "id" field,
 * since that field and its accessors are inherited from {@link Class4}.
 * The class is used to check if the generated code for instantiating a {@link Class5} object
 * (to set just its id) calls the inherited {@code setId} method instead of accessing the private field directly.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class6 {
    private long id;

    @DTO.MapToId
    private Class5 class5;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Class5 getClass5() {
        return class5;
    }

    public void setClass5(Class5 class5) {
        this.class5 = class5;
    }
}
