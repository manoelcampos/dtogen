package io.github.manoelcampos.dtogen.sample.model;

import io.github.manoelcampos.dtogen.DTO;

/**
 * @author Manoel Campos
 */
@DTO
public record ProfessionRecord(long id, String name, String description) {
}
