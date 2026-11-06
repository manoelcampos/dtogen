package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;
import io.github.manoelcampos.dtogen.samples.other.Class12;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A model class with generic fields whose type arguments are from other packages:
 * a {@link List} of a class annotated with {@link DTO} and a {@link Map} with nested type arguments.
 * It also has a field whose type is from a {@code java.lang} subpackage, which must be imported.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class11 {
    private Long id;
    private List<Class12> items;
    private Map<String, List<LocalDate>> dates;
    private Type type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Class12> getItems() {
        return items;
    }

    public void setItems(List<Class12> items) {
        this.items = items;
    }

    public Map<String, List<LocalDate>> getDates() {
        return dates;
    }

    public void setDates(Map<String, List<LocalDate>> dates) {
        this.dates = dates;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
