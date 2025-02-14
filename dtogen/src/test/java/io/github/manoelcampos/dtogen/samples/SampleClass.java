package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

import java.util.List;

/**
 * A simple class that doesn't have any association with other classes/records.
 * @author Manoel Campos
 */
@DTO
@SuppressWarnings({"rawtypes", "unused"})
public class SampleClass {
    private String str;
    private boolean bool;
    private List<String> genericList;
    private List nonGenericList;
}
