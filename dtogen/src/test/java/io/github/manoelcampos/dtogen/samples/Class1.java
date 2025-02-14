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
}
