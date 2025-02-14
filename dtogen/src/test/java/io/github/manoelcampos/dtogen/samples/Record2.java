package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

import java.time.LocalTime;

/**
 * A record that has a field of another record type, annotated with {@link DTO.MapToId}
 * @author Manoel Campos
 */
@DTO
public record Record2(Long id, LocalTime time, double value, @DTO.MapToId Record3 record3) {
}
