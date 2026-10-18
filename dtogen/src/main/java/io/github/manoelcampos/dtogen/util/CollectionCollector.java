package io.github.manoelcampos.dtogen.util;

import javax.lang.model.element.VariableElement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines the code to collect a stream into a collection of the same type of a given collection field.
 * That collection field is a model field whose generic type argument is annotated with {@link io.github.manoelcampos.dtogen.DTO},
 * so that the generated DTO record maps each item from the model to a DTO object (and the other way around).
 *
 * <p>The values are declared from the most specific to the most generic collection type,
 * since {@link #of(TypeUtil, VariableElement)} returns the first value whose type is assignable from the field type.</p>
 * @author Manoel Campos
 */
public enum CollectionCollector {
    SORTED_SET(SortedSet.class, ".collect(Collectors.toCollection(TreeSet::new))", TreeSet.class),

    /**
     * Uses a {@link LinkedHashSet} to keep the order of the items.
     */
    SET(Set.class, ".collect(Collectors.toCollection(LinkedHashSet::new))", LinkedHashSet.class),

    /**
     * The default collector for a {@link List}, {@link Collection} or any other type not matched by other values.
     */
    LIST(Collection.class, ".toList()");

    private final Class<?> collectionType;
    private final String code;
    private final List<String> imports;

    CollectionCollector(final Class<?> collectionType, final String code, final Class<?> collectionImplementation) {
        this.collectionType = collectionType;
        this.code = code;
        this.imports = Stream.of(Collectors.class, collectionImplementation).map(Class::getName).toList();
    }

    CollectionCollector(final Class<?> collectionType, final String code) {
        this.collectionType = collectionType;
        this.code = code;
        this.imports = List.of();
    }

    /**
     * {@return the collector matching the type of a given collection field}
     * If no collector matches the field type, {@link #LIST} is returned.
     * @param typeUtil a {@link TypeUtil} instance to check the field type
     * @param collectionField the collection field to get the collector for
     */
    public static CollectionCollector of(final TypeUtil typeUtil, final VariableElement collectionField) {
        return Arrays.stream(values())
                     .filter(collector -> typeUtil.isSubtypeOf(collectionField, collector.collectionType))
                     .findFirst()
                     .orElse(LIST);
    }

    /**
     * {@return the code to be appended to a stream chain to collect its items}
     */
    public String code() {
        return code;
    }

    /**
     * {@return the fully qualified name of the classes to be imported to use the {@link #code()}}
     */
    public List<String> imports() {
        return imports;
    }
}
