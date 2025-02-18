package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class containing an association with another class.
 * The class is used to check if the related field is correctly included in the generated DTO record.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class1 {
    public static final int MAX = Integer.MAX_VALUE;

    private long id;
    private Class2 class2;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Class2 getClass2() {
        return class2;
    }

    public void setClass2(Class2 class2) {
        this.class2 = class2;
    }
}
