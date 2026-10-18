package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

import java.util.*;

/**
 * A model class with collection fields of different types ({@link List}, {@link Collection}, {@link Set} and {@link SortedSet}),
 * whose generic type argument is annotated with {@link DTO}.
 * The class is used to check if the stream collector used to map items between DTOs and models
 * matches the type of each collection field, so that the generated DTO record compiles.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class10 {
    private List<Class7> list;
    private Collection<Class7> collection;
    private Set<Class7> set;
    private SortedSet<Class7> sortedSet;

    public List<Class7> getList() {
        return list;
    }

    public void setList(List<Class7> list) {
        this.list = list;
    }

    public Collection<Class7> getCollection() {
        return collection;
    }

    public void setCollection(Collection<Class7> collection) {
        this.collection = collection;
    }

    public Set<Class7> getSet() {
        return set;
    }

    public void setSet(Set<Class7> set) {
        this.set = set;
    }

    public SortedSet<Class7> getSortedSet() {
        return sortedSet;
    }

    public void setSortedSet(SortedSet<Class7> sortedSet) {
        this.sortedSet = sortedSet;
    }
}
