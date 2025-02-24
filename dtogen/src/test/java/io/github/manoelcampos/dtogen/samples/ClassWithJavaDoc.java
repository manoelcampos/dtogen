package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

@SuppressWarnings("unused")
@DTO
public class ClassWithJavaDoc {
    public static final int MIN = Integer.MIN_VALUE; // Constant with no JavaDoc

    /**
     * A MAX constant.
     */
    public static final int MAX = Integer.MAX_VALUE;

    /**
     * The id of the object.
     */
    private long id;

    /**
     * The name of the object.
     * <p>This is used to provide a user-friendly representation when printing the object.</p>
     */
    private String name;

    private double value; // Field with no JavaDoc
}
