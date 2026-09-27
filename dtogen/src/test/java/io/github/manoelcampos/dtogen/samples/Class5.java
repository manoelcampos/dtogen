package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A model class with no fields of its own, whose "id" field and accessors are inherited from {@link Class4}.
 * It's used as the target of a {@link DTO.MapToId} association in {@link Class6},
 * to check if the DTO generator recognizes the {@code setId}/{@code getId} methods inherited from the superclass.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class5 extends Class4 {
}
