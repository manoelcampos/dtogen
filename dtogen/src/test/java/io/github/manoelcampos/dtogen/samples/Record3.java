package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

/**
 * A simple record used as attribute of other records/classes.
 * @author Manoel Campos
 */
@DTO
public record Record3(Long id, char letter) {
}
