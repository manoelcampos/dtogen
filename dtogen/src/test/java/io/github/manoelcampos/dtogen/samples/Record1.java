package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

import java.time.LocalDate;

/**
 * A simple record with no associations.
 * @author Manoel Campos
 */
@DTO
public record Record1(Long id, String name, LocalDate date) {
}
