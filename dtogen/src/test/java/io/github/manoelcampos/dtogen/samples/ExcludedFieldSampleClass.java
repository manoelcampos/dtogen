package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class that uses the {@link DTO.Exclude} annotation
 * to check if the DTO record generated ignores such fields.
 * @author Manoel Campos
 */
@DTO
@SuppressWarnings("unused")
public class ExcludedFieldSampleClass {
    private boolean included;

    @DTO.Exclude
    private boolean excluded;
}
