package io.github.manoelcampos.dtogen.samples;

import java.util.Objects;
import java.util.function.Supplier;

/// An interface with this same name and structure is generated when the DTOGen Processor is executed.
/// But it was replicated here to be used in the tests,
/// without requiring the annotation processing to be
/// performed during test execution (which complicates tests setup).
/// @author Manoel Campos
public interface DTORecord<T> {
    T toModel();

    DTORecord<T> fromModel(T model);

    default <O> O newObject(final Object id, final Supplier<O> supplier){
        return Objects.isNull(id) ? null : supplier.get();
    }
}
