package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTORecord;
import javax.annotation.processing.Generated;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import java.util.List;
import java.util.SortedSet;
import java.util.Collection;
import java.util.Set;

/// A sample of the DTO record that the DTOGen must generate for the [Class10] model.
/// This DTO is used in tests to check if the DTO is generated as expected and compiles correctly.
///
/// Comments starting with /// are stripped out from the code when this file is read during test execution.
/// These comments won't be present inside the generated DTO record that is expected to be equal to this one.

/**
 * A {@link DTORecord Data Transfer Object} for {@link Class10}.
 */
@Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
public record Class10DTO (List<Class7DTO> list, Collection<Class7DTO> collection, Set<Class7DTO> set, SortedSet<Class7DTO> sortedSet) implements DTORecord<Class10> {
    @Override
    public Class10 toModel(){
        final var model = new Class10();
        model.setList(list.stream().map(Class7DTO::toModel).toList());
        model.setCollection(collection.stream().map(Class7DTO::toModel).toList());
        model.setSet(set.stream().map(Class7DTO::toModel).collect(Collectors.toCollection(LinkedHashSet::new)));
        model.setSortedSet(sortedSet.stream().map(Class7DTO::toModel).collect(Collectors.toCollection(TreeSet::new)));

        return model;
    }

    @Override
    public Class10DTO fromModel(final Class10 model){
        final var dto = new Class10DTO(
            model.getList().stream().map(item -> new Class7DTO().fromModel(item)).toList(),
            model.getCollection().stream().map(item -> new Class7DTO().fromModel(item)).toList(),
            model.getSet().stream().map(item -> new Class7DTO().fromModel(item)).collect(Collectors.toCollection(LinkedHashSet::new)),
            model.getSortedSet().stream().map(item -> new Class7DTO().fromModel(item)).collect(Collectors.toCollection(TreeSet::new))
        );

        return dto;
    }

    public Class10DTO() {
        this(null, null, null, null);
    }
}
