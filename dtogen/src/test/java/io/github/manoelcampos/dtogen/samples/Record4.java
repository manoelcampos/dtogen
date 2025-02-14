package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A record that has a field of a class type, annotated with {@link DTO.MapToId}
 * @author Manoel Campos
 */
@DTO
public record Record4(Long id, double width, @DTO.MapToId Class1 class1) {
}
