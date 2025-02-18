package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class containing an association with another class,
 * where the association field is annotated with {@link DTO.MapToId}.
 * The class is used to check if the id of the related field is included in the generated DTO record as "class3Id".
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class2 {
    private long id;

    @DTO.MapToId
    private Class3 class3;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Class3 getClass3() {
        return class3;
    }

    public void setClass3(Class3 class3) {
        this.class3 = class3;
    }
}
