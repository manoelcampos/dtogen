/// This interface is copied to the resources dir of the DTOGen jar.
/// Since the DTOGen is not a runtime dependency in the application that uses it,
/// the application cannot access the DTORecord.class compiled in the DTOGen jar.
/// Alternatively, we need to include the DTORecord.java source file in the jar
/// so that when the DTOGen processor is executed during the application compilation,
/// the DTORecord.java source file is copied to the application generated-sources dir.
///
/// This strategy avoid defining the DTORecord source code as a String,
/// which has no way to be checked by the compiler.
/// Therefore, if the DTORecord.java is a regular file,
/// when the DTOGen project is built, the file is built too.
///
/// Comments starting with /// are striped out from the code when this file is read from the DTOGen jar resources dir.
package io.github.manoelcampos.dtogen;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Interface to be implemented by DTO records.
 * @param <T> the type of the model object this DTO record represents
 * @author Manoel Campos
 */
public interface DTORecord<T> {
    T toModel();
    DTORecord<T> fromModel(T model);

    /**
     * {@return a new model object (which may be different from the DTO model (T))}
     * It sets some attributes inside a supplier function.
     * @param supplier the supplier function to create the object and set some attributes
     */
    default <O> O newObject(final Object id, final Supplier<O> supplier){
        return hasId(id) ? supplier.get() : null;
    }

    default boolean hasId(final Object id){
        // Works if the id field is a primitive/boxed-type (such as long or Long) or any object
        if (Objects.isNull(id)) {
            return false;
        }

        if(id instanceof Number){
            return ((Number) id).doubleValue() > 0;
        }

        return !id.toString().isBlank();
    }
}
